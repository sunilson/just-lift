package at.sunilson.justlift.features.workout.data

import android.util.Log
import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import at.sunilson.justlift.features.workout.data.WorkoutSettingsRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Vitruvian device BLE manager using Kable (https://github.com/JuulLabs/kable).
 *
 * Notes:
 * - This implementation assumes Bluetooth permissions are already granted (per issue statement).
 * - Minimal functionality: scan, connect, and start a "Just Lift" Echo workout by sending
 *   the INIT sequence followed by the Echo control frame.
 */
@Single
class VitruvianDeviceManagerImpl(
    private val workoutSettingsRepository: WorkoutSettingsRepository
) : VitruvianDeviceManager {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun ensureMachineJobRunning(device: Peripheral) {
        val session = sessionFor(device)
        if (session.machineJob?.isActive == true) return
        session.machineJob = scope.launch {
            while (isActive) {
                try {
                    val data = device.read(MONITOR_CHARACTERISTIC)

                    // Parse loads (kg)
                    val (leftKg, rightKg) = parseMonitorLoads(data)

                    // Parse positions (raw u16), filter spikes, and normalize 0.0..1.0
                    val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                    var rawPosA = if (data.size >= 12) (bb.getShort(4).toInt() and 0xFFFF) else 0 // Right
                    var rawPosB = if (data.size >= 16) (bb.getShort(10).toInt() and 0xFFFF) else 0 // Left
                    if (rawPosA > POSITION_SPIKE_FILTER_MAX) {
                        rawPosA = session.lastGoodPosA
                    } else {
                        session.lastGoodPosA = rawPosA
                    }
                    if (rawPosB > POSITION_SPIKE_FILTER_MAX) {
                        rawPosB = session.lastGoodPosB
                    } else {
                        session.lastGoodPosB = rawPosB
                    }
                    val posRight = (rawPosA / POSITION_NORMALIZATION_DIVISOR_MM).coerceIn(0.0, 1.0)
                    val posLeft = (rawPosB / POSITION_NORMALIZATION_DIVISOR_MM).coerceIn(0.0, 1.0)

                    // Detect movement based on position deltas (filtering out tiny jitter)
                    var deltaL = 0.0
                    var deltaR = 0.0
                    val moving: Boolean = if (!session.prevPosInitialized) {
                        session.prevPosInitialized = true
                        session.prevPosLeft = posLeft
                        session.prevPosRight = posRight
                        false
                    } else {
                        deltaL = kotlin.math.abs(posLeft - session.prevPosLeft)
                        deltaR = kotlin.math.abs(posRight - session.prevPosRight)
                        session.prevPosLeft = posLeft
                        session.prevPosRight = posRight
                        (deltaL + deltaR) > MOVEMENT_DELTA_THRESHOLD
                    }

                    // Track velocity (combined normalized pos change per second)
                    val velocity = (deltaL + deltaR) / (MONITOR_INTERVAL_MS / 1000.0)
                    if (velocity > session.repMaxVelocity) session.repMaxVelocity = velocity

                    // Throttled debug logging for diagnosing position scaling
                    val nowTs = System.currentTimeMillis()
                    if (nowTs - session.lastLogAtMillis > POSITION_LOG_THROTTLE_MS) {
                        session.lastLogAtMillis = nowTs
                    }

                    // Emit machine state
                    session.machineState.value = VitruvianDeviceManager.MachineState(
                        forceLeftCable = leftKg,
                        forceRightCable = rightKg,
                        positionCableLeft = posLeft,
                        positionCableRight = posRight
                    )

                    // If workout is active and calibration finished, accumulate force metrics per phase
                    val activeState = session.state.value
                    if (activeState != null && session.calibrationRepsCompleted >= CALIBRATION_REPS) {
                        val phase = session.currentPhase
                        if (phase != null) {
                            val combined = (leftKg + rightKg) / 2.0
                            if (moving) {
                                // First movement in a new phase identifies the end of the "rest" period
                                if (session.repStartedMovingAt == null) {
                                    val now = System.currentTimeMillis()
                                    session.repStartedMovingAt = now
                                    session.lastRepNotificationAtMillis?.let { lastNotif ->
                                        session.avgRestDurationSum += (now - lastNotif)
                                        session.avgRestDurationCount += 1
                                    }
                                }

                                // Track peak force and its position for the current rep/phase
                                if (combined > session.repPeakForce) {
                                    session.repPeakForce = combined
                                    session.repPeakForcePos = (posLeft + posRight) / 2.0
                                }

                                when (phase) {
                                    Phase.UP -> {
                                        session.upForceSum += combined
                                        session.upForceLeftSum += leftKg
                                        session.upForceRightSum += rightKg
                                        session.upForceCount += 1
                                        if (combined > session.maxUpForce) session.maxUpForce = combined
                                    }
                                    Phase.DOWN -> {
                                        session.downForceSum += combined
                                        session.downForceLeftSum += leftKg
                                        session.downForceRightSum += rightKg
                                        session.downForceCount += 1
                                        if (combined > session.maxDownForce) session.maxDownForce = combined
                                    }
                                }
                            }

                            if (posLeft < session.minPosLeft) session.minPosLeft = posLeft
                            if (posLeft > session.maxPosLeft) session.maxPosLeft = posLeft
                            if (posRight < session.minPosRight) session.minPosRight = posRight
                            if (posRight > session.maxPosRight) session.maxPosRight = posRight

                            val avgUp = if (session.upForceCount > 0) session.upForceSum / session.upForceCount else 0.0
                            val avgDown = if (session.downForceCount > 0) session.downForceSum / session.downForceCount else 0.0

                            val avgUpLeft = if (session.upForceCount > 0) session.upForceLeftSum / session.upForceCount else 0.0
                            val avgUpRight = if (session.upForceCount > 0) session.upForceRightSum / session.upForceCount else 0.0
                            val avgDownLeft = if (session.downForceCount > 0) session.downForceLeftSum / session.downForceCount else 0.0
                            val avgDownRight = if (session.downForceCount > 0) session.downForceRightSum / session.downForceCount else 0.0

                            session.state.value = activeState.copy(
                                averageUpwardForce = avgUp,
                                averageDownwardForce = avgDown,
                                maxUpwardForce = session.maxUpForce,
                                maxDownwardForce = session.maxDownForce,
                                averageUpwardForceLeft = avgUpLeft,
                                averageUpwardForceRight = avgUpRight,
                                averageDownwardForceLeft = avgDownLeft,
                                averageDownwardForceRight = avgDownRight,
                                minPositionLeft = session.minPosLeft,
                                maxPositionLeft = session.maxPosLeft,
                                minPositionRight = session.minPosRight,
                                maxPositionRight = session.maxPosRight,
                                avgMinPositionLeft = if (session.minPosLeftCount > 0) session.minPosLeftSum / session.minPosLeftCount else 0.0,
                                avgMaxPositionLeft = if (session.maxPosLeftCount > 0) session.maxPosLeftSum / session.maxPosLeftCount else 0.0,
                                avgMinPositionRight = if (session.minPosRightCount > 0) session.minPosRightSum / session.minPosRightCount else 0.0,
                                avgMaxPositionRight = if (session.maxPosRightCount > 0) session.maxPosRightSum / session.maxPosRightCount else 0.0,
                                avgUpwardRepDurationMillis = session.upDurAvg,
                                avgDownwardRepDurationMillis = session.downDurAvg,
                                avgUpwardPeakForcePosition = session.upPeakForcePosAvg,
                                avgDownwardPeakForcePosition = session.downPeakForcePosAvg,
                                avgUpwardMaxVelocity = session.upMaxVelocityAvg,
                                avgDownwardMaxVelocity = session.downMaxVelocityAvg,
                                avgRestDurationMillis = session.restDurationAvg
                            )
                        }
                    }

                    // Auto-stop logic: trigger hold-based stop when either
                    // - both cables are near bottom, OR
                    // - both cables are at or below the light-load threshold (<= 2.6 kg)
                    // Note: Force-based check is disabled during calibration because loads hover ~2.5–2.6 kg.
                    val active = session.state.value != null
                    if (active) {
                        val atBottomLeft = posLeft <= BOTTOM_POS_THRESHOLD
                        val atBottomRight = posRight <= BOTTOM_POS_THRESHOLD
                        val isCalibrating = session.calibrationRepsCompleted < CALIBRATION_REPS
                        // Only allow force-based auto-stop after calibration has completed
                        val lightLoadBoth = !isCalibrating && (leftKg <= FORCE_AUTO_STOP_KG && rightKg <= FORCE_AUTO_STOP_KG)
                        val shouldAutoStop = (atBottomLeft && atBottomRight) || lightLoadBoth

                        if (shouldAutoStop) {
                            val now = System.currentTimeMillis()
                            if (session.bottomHoldSince == null) {
                                session.bottomHoldSince = now
                            }
                            val elapsed = now - (session.bottomHoldSince ?: now)
                            val remainingMs = AUTO_STOP_HOLD_MS - elapsed
                            val secondsLeft = if (remainingMs > 0) ceil(remainingMs / 1000.0).toInt() else 0

                            // Update countdown in workout state
                            session.state.value = session.state.value?.copy(autoStopInSeconds = secondsLeft)

                            if (elapsed >= AUTO_STOP_HOLD_MS) {
                                // Reset timer before stopping to avoid loops
                                session.bottomHoldSince = null
                                // Best-effort stop
                                try {
                                    stopWorkout(device)
                                } catch (_: Throwable) {
                                }
                            }
                        } else {
                            session.bottomHoldSince = null
                            // Clear countdown if previously set
                            if (session.state.value?.autoStopInSeconds != null) {
                                session.state.value = session.state.value?.copy(autoStopInSeconds = null)
                            }
                        }
                    } else {
                        // No active workout, ensure countdown cleared
                        if (session.state.value?.autoStopInSeconds != null) {
                            session.state.value = session.state.value?.copy(autoStopInSeconds = null)
                        }
                        session.bottomHoldSince = null
                    }
                } catch (_: Throwable) {
                    // Likely disconnected or transient read failure; keep trying at a slower pace
                }
                delay(MONITOR_INTERVAL_MS)
            }
        }
    }

    // Per-device workout session state
    private data class WorkoutSession(
        var startedAtMillis: Long = 0L,
        var maxReps: Int? = null,
        val state: MutableStateFlow<VitruvianDeviceManager.WorkoutState?> = MutableStateFlow(null),
        // Machine state is per device and should emit regardless of workout
        val machineState: MutableStateFlow<VitruvianDeviceManager.MachineState?> = MutableStateFlow(null),
        var upwardReps: Int = 0,
        var downwardReps: Int = 0,
        var halfRepNotifications: Int = 0,
        var calibrationRepsCompleted: Int = 0,
        var machineJob: Job? = null,
        var timeJob: Job? = null,
        var repJob: Job? = null,
        // Position parsing helpers
        var lastGoodPosA: Int = 0,
        var lastGoodPosB: Int = 0,
        // Movement detection
        var prevPosLeft: Double = 0.0,
        var prevPosRight: Double = 0.0,
        var prevPosInitialized: Boolean = false,
        // Auto-stop detection
        var bottomHoldSince: Long? = null,
        // Debug logging throttle
        var lastLogAtMillis: Long = 0L,
        // Behavior flags
        var stopAtLastTopRep: Boolean = false,
        // Preparation
        var lastPreparedAtMillis: Long = 0L,
        // Force metrics accumulation
        var upForceSum: Double = 0.0,
        var upForceCount: Long = 0,
        var downForceSum: Double = 0.0,
        var downForceCount: Long = 0,
        var maxUpForce: Double = 0.0,
        var maxDownForce: Double = 0.0,
        var upForceLeftSum: Double = 0.0,
        var upForceRightSum: Double = 0.0,
        var downForceLeftSum: Double = 0.0,
        var downForceRightSum: Double = 0.0,
        var minPosLeft: Double = 1.0,
        var maxPosLeft: Double = 0.0,
        var minPosRight: Double = 1.0,
        var maxPosRight: Double = 0.0,
        var minPosLeftSum: Double = 0.0,
        var minPosLeftCount: Long = 0,
        var maxPosLeftSum: Double = 0.0,
        var maxPosLeftCount: Long = 0,
        var minPosRightSum: Double = 0.0,
        var minPosRightCount: Long = 0,
        var maxPosRightSum: Double = 0.0,
        var maxPosRightCount: Long = 0,
        var lastRepNotificationAtMillis: Long? = null,
        var upwardDurationSum: Long = 0,
        var upwardDurationCount: Long = 0,
        var downwardDurationSum: Long = 0,
        var downwardDurationCount: Long = 0,
        var avgUpwardPeakForcePosSum: Double = 0.0,
        var avgUpwardPeakForcePosCount: Long = 0,
        var avgDownwardPeakForcePosSum: Double = 0.0,
        var avgDownwardPeakForcePosCount: Long = 0,
        var avgUpwardMaxVelocitySum: Double = 0.0,
        var avgUpwardMaxVelocityCount: Long = 0,
        var avgDownwardMaxVelocitySum: Double = 0.0,
        var avgDownwardMaxVelocityCount: Long = 0,
        var avgRestDurationSum: Long = 0,
        var avgRestDurationCount: Long = 0,

        // Per-rep trackers (reset on phase change)
        var repPeakForce: Double = 0.0,
        var repPeakForcePos: Double = 0.0,
        var repMaxVelocity: Double = 0.0,
        var repStartedMovingAt: Long? = null,

        var currentPhase: Phase? = null,
        var difficulty: VitruvianDeviceManager.EchoDifficulty = VitruvianDeviceManager.EchoDifficulty.WARMUP
    ) {
        val upDurAvg: Double
            get() = if (upwardDurationCount > 0) upwardDurationSum.toDouble() / upwardDurationCount else 0.0

        val downDurAvg: Double
            get() = if (downwardDurationCount > 0) downwardDurationSum.toDouble() / downwardDurationCount else 0.0

        val upPeakForcePosAvg: Double
            get() = if (avgUpwardPeakForcePosCount > 0) avgUpwardPeakForcePosSum / avgUpwardPeakForcePosCount else 0.0

        val downPeakForcePosAvg: Double
            get() = if (avgDownwardPeakForcePosCount > 0) avgDownwardPeakForcePosSum / avgDownwardPeakForcePosCount else 0.0

        val upMaxVelocityAvg: Double
            get() = if (avgUpwardMaxVelocityCount > 0) avgUpwardMaxVelocitySum / avgUpwardMaxVelocityCount else 0.0

        val downMaxVelocityAvg: Double
            get() = if (avgDownwardMaxVelocityCount > 0) avgDownwardMaxVelocitySum / avgDownwardMaxVelocityCount else 0.0

        val restDurationAvg: Double
            get() = if (avgRestDurationCount > 0) avgRestDurationSum.toDouble() / avgRestDurationCount else 0.0
    }

    private enum class Phase { UP, DOWN }

    private val sessions = mutableMapOf<String, WorkoutSession>()
    private fun sessionFor(device: Peripheral): WorkoutSession =
        sessions.getOrPut(device.identifier) { WorkoutSession() }

    override fun getScannedDevicesFlow(): Flow<List<Peripheral>> {
        return Scanner().advertisements
            .mapNotNull { adv: Advertisement ->
                if (adv.name?.startsWith("Vee") == true) {
                    Peripheral(adv)
                } else null
            }
            .runningFold(mapOf<String, Peripheral>()) { acc, peripheral -> acc + (peripheral.identifier to peripheral) }
            .mapNotNull { it.values.toList() }

    }

    override fun getWorkoutStateFlow(device: Peripheral): Flow<VitruvianDeviceManager.WorkoutState?> {
        return sessionFor(device).state
    }

    override fun getMachineStateFlow(device: Peripheral): Flow<VitruvianDeviceManager.MachineState?> {
        val session = sessionFor(device)
        ensureMachineJobRunning(device)
        return session.machineState
    }

    override suspend fun startWorkout(
        userId: Int,
        device: Peripheral,
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        eccentricPercentage: Double,
        maxReps: Int?,
        stopAtLastTopRep: Boolean
    ) {
        Log.d("ExerciseRecognition", "startWorkout: userId=$userId, difficulty=$difficulty, eccentric=$eccentricPercentage, maxReps=$maxReps")
        val session = sessionFor(device)
        val now = System.currentTimeMillis()
        // If not recently prepared, send INIT + PRESET to ensure fast start
        if (now - session.lastPreparedAtMillis > PREPARE_VALID_MS) {
            writeWithResponse(device, RX_CHARACTERISTIC, buildInitCommand())
            delay(50)
            writeWithResponse(device, RX_CHARACTERISTIC, buildInitPreset())
            session.lastPreparedAtMillis = now
        }

        // Compute eccentric percentage from ratio 0.0–1.3; coerce out-of-range inputs.
        val eccentricRatio = eccentricPercentage.coerceIn(0.0, 1.3)
        val eccentricPctInt = (eccentricRatio * 100.0).roundToInt().coerceIn(0, 130)

        // Resolve per-mode machine parameters (gain, cap) from repository
        val modeParams = workoutSettingsRepository.getModeParameters(userId, difficulty)

        // Build Echo control frame using chosen difficulty and tuned parameters.
        val frame = buildEchoControlFrame(
            level = difficulty,
            // If maxReps is null we use 0xFF which means unlimited (Just Lift)
            targetReps = maxReps,
            eccentricPct = eccentricPctInt,
            gain = modeParams.gain,
            cap = modeParams.capKg
        )
        writeWithResponse(device, RX_CHARACTERISTIC, frame)

        // Start local session tracking of workout state
        // Cancel any existing jobs for safety
        session.timeJob?.cancel()
        session.repJob?.cancel()
        session.startedAtMillis = System.currentTimeMillis()
        session.maxReps = maxReps
        session.upwardReps = 0
        session.downwardReps = 0
        session.halfRepNotifications = 0
        session.calibrationRepsCompleted = 0
        session.stopAtLastTopRep = stopAtLastTopRep
        // Reset force metrics
        session.upForceSum = 0.0
        session.upForceCount = 0
        session.downForceSum = 0.0
        session.downForceCount = 0
        session.maxUpForce = 0.0
        session.maxDownForce = 0.0
        session.upForceLeftSum = 0.0
        session.upForceRightSum = 0.0
        session.downForceLeftSum = 0.0
        session.downForceRightSum = 0.0
        session.minPosLeft = 1.0
        session.maxPosLeft = 0.0
        session.minPosRight = 1.0
        session.maxPosRight = 0.0
        session.minPosLeftSum = 0.0
        session.minPosLeftCount = 0
        session.maxPosLeftSum = 0.0
        session.maxPosLeftCount = 0
        session.minPosRightSum = 0.0
        session.minPosRightCount = 0
        session.maxPosRightSum = 0.0
        session.maxPosRightCount = 0
        session.lastRepNotificationAtMillis = null
        session.upwardDurationSum = 0
        session.upwardDurationCount = 0
        session.downwardDurationSum = 0
        session.downwardDurationCount = 0
        session.avgUpwardPeakForcePosSum = 0.0
        session.avgUpwardPeakForcePosCount = 0
        session.avgDownwardPeakForcePosSum = 0.0
        session.avgDownwardPeakForcePosCount = 0
        session.avgUpwardMaxVelocitySum = 0.0
        session.avgUpwardMaxVelocityCount = 0
        session.avgDownwardMaxVelocitySum = 0.0
        session.avgDownwardMaxVelocityCount = 0
        session.avgRestDurationSum = 0
        session.avgRestDurationCount = 0
        session.repPeakForce = 0.0
        session.repPeakForcePos = 0.0
        session.repMaxVelocity = 0.0
        session.repStartedMovingAt = null
        session.currentPhase = null
        session.difficulty = difficulty
        // Reset movement detection so first tick doesn't count as movement by accident
        session.prevPosInitialized = false
        session.prevPosLeft = 0.0
        session.prevPosRight = 0.0
        session.state.value = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = session.calibrationRepsCompleted,
            maxReps = maxReps,
            upwardRepetitionsCompleted = 0,
            downwardRepetitionsCompleted = 0,
            timeElapsed = Duration.ZERO,
            difficulty = difficulty
        )

        // Ensure machine polling is running to keep forces updated globally
        ensureMachineJobRunning(device)

        // Update elapsed time every 100ms while workout is active
        session.timeJob = scope.launch {
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - session.startedAtMillis).milliseconds
                val curr = session.state.value ?: break
                session.state.value = curr.copy(timeElapsed = elapsed)
                delay(100)
            }
        }

        // Subscribe to rep notifications (each notification = half-rep: top or bottom)
        session.repJob = scope.launch {
            try {
                device.observe(REP_NOTIFY_CHARACTERISTIC).collect {
                    val curr = session.state.value ?: return@collect

                    // Increment half-rep counter to derive top/bottom parity
                    session.halfRepNotifications += 1
                    val isUpwardCompletion = (session.halfRepNotifications % 2 == 1)

                    val now = System.currentTimeMillis()
                    val lastTs = session.lastRepNotificationAtMillis
                    if (lastTs != null) {
                        val duration = now - lastTs
                        if (isUpwardCompletion) {
                            session.upwardDurationSum += duration
                            session.upwardDurationCount += 1
                        } else {
                            session.downwardDurationSum += duration
                            session.downwardDurationCount += 1
                        }
                    }
                    session.lastRepNotificationAtMillis = now

                    // During calibration, perform 3 reps counted at the TOP (upward completion)
                    // These calibration reps do not contribute to normal rep totals
                    if (session.calibrationRepsCompleted < CALIBRATION_REPS) {
                        if (isUpwardCompletion) {
                            // Count calibration rep when TOP is reached
                            session.calibrationRepsCompleted += 1
                            Log.d("ExerciseRecognition", "Calibration rep completed: ${session.calibrationRepsCompleted}")
                            session.state.value = curr.copy(calibratingRepsCompleted = session.calibrationRepsCompleted)
                            // After finishing calibration at TOP, next movement will be DOWN
                            if (session.calibrationRepsCompleted >= CALIBRATION_REPS) {
                                session.currentPhase = Phase.DOWN
                            }
                        }
                        // Do not count reps during calibration
                        return@collect
                    }

                    if (isUpwardCompletion) {
                        // Finished UP phase
                        if (session.repPeakForce > 0) {
                            session.avgUpwardPeakForcePosSum += session.repPeakForcePos
                            session.avgUpwardPeakForcePosCount += 1
                        }
                        session.avgUpwardMaxVelocitySum += session.repMaxVelocity
                        session.avgUpwardMaxVelocityCount += 1

                        // Top reached -> increment upward counter (shown to user)
                        session.upwardReps += 1
                        Log.d("ExerciseRecognition", "Upward rep completed: ${session.upwardReps}")
                        // Switch to DOWN phase after reaching the top
                        session.currentPhase = Phase.DOWN
                    } else {
                        // Finished DOWN phase
                        if (session.repPeakForce > 0) {
                            session.avgDownwardPeakForcePosSum += session.repPeakForcePos
                            session.avgDownwardPeakForcePosCount += 1
                        }
                        session.avgDownwardMaxVelocitySum += session.repMaxVelocity
                        session.avgDownwardMaxVelocityCount += 1

                        // Bottom reached -> increment downward counter and check for auto-stop
                        session.downwardReps += 1
                        Log.d("ExerciseRecognition", "Downward rep completed: ${session.downwardReps}")
                        // Switch to UP phase after reaching the bottom
                        session.currentPhase = Phase.UP
                    }

                    // Reset per-rep trackers
                    session.repPeakForce = 0.0
                    session.repPeakForcePos = 0.0
                    session.repMaxVelocity = 0.0
                    session.repStartedMovingAt = null

                    // Record positions at rep completion to calculate average peak positions
                    val machineState = session.machineState.value
                    if (machineState != null) {
                        if (isUpwardCompletion) {
                            session.maxPosLeftSum += machineState.positionCableLeft
                            session.maxPosLeftCount += 1
                            session.maxPosRightSum += machineState.positionCableRight
                            session.maxPosRightCount += 1
                        } else {
                            session.minPosLeftSum += machineState.positionCableLeft
                            session.minPosLeftCount += 1
                            session.minPosRightSum += machineState.positionCableRight
                            session.minPosRightCount += 1
                        }
                    }

                    val avgMinL = if (session.minPosLeftCount > 0) session.minPosLeftSum / session.minPosLeftCount else 0.0
                    val avgMaxL = if (session.maxPosLeftCount > 0) session.maxPosLeftSum / session.maxPosLeftCount else 0.0
                    val avgMinR = if (session.minPosRightCount > 0) session.minPosRightSum / session.minPosRightCount else 0.0
                    val avgMaxR = if (session.maxPosRightCount > 0) session.maxPosRightSum / session.maxPosRightCount else 0.0

                    val updated = curr.copy(
                        calibratingRepsCompleted = CALIBRATION_REPS,
                        upwardRepetitionsCompleted = session.upwardReps,
                        downwardRepetitionsCompleted = session.downwardReps,
                        avgMinPositionLeft = avgMinL,
                        avgMaxPositionLeft = avgMaxL,
                        avgMinPositionRight = avgMinR,
                        avgMaxPositionRight = avgMaxR,
                        avgUpwardRepDurationMillis = session.upDurAvg,
                        avgDownwardRepDurationMillis = session.downDurAvg,
                        avgUpwardPeakForcePosition = session.upPeakForcePosAvg,
                        avgDownwardPeakForcePosition = session.downPeakForcePosAvg,
                        avgUpwardMaxVelocity = session.upMaxVelocityAvg,
                        avgDownwardMaxVelocity = session.downMaxVelocityAvg,
                        avgRestDurationMillis = session.restDurationAvg
                    )
                    session.state.value = updated

                    val target = session.maxReps
                    if (target != null) {
                        if (session.stopAtLastTopRep) {
                            // Stop on upward completion when upward reps reach target
                            if (isUpwardCompletion && session.upwardReps >= target) {
                                stopWorkout(device)
                            }
                        } else {
                            // Default: stop on downward completion when bottom reps reach target + 1 (stop at bottom after last top)
                            if (!isUpwardCompletion && session.downwardReps >= target + 1) {
                                stopWorkout(device)
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
                // Observation ends
            }
        }
    }

    override suspend fun prepareForWorkout(device: Peripheral) {
        val session = sessionFor(device)
        val now = System.currentTimeMillis()
        if (now - session.lastPreparedAtMillis > PREPARE_THROTTLE_MS) {
            try {
                writeWithResponse(device, RX_CHARACTERISTIC, buildInitCommand())
                delay(50)
                writeWithResponse(device, RX_CHARACTERISTIC, buildInitPreset())
                session.lastPreparedAtMillis = now
            } catch (_: Throwable) {
                // Ignore pre-init errors; we'll try again on start
            }
        }
    }

    override suspend fun stopWorkout(device: Peripheral) {
        Log.d("ExerciseRecognition", "stopWorkout: device=${device.identifier}")
        // Send STOP command (same as INIT command in JS reference)
        try {
            writeWithResponse(device, RX_CHARACTERISTIC, buildInitCommand())
        } catch (_: Throwable) {
            // Ignore write errors on stop
        }
        val session = sessionFor(device)
        session.timeJob?.cancel()
        session.repJob?.cancel()
        session.timeJob = null
        session.repJob = null
        session.state.value = null
        // Keep machineJob running so forces continue to be emitted when idle
        ensureMachineJobRunning(device)
    }

    // --- BLE + Protocol helpers -----------------------------------------------------------------

    private suspend fun writeWithResponse(peripheral: Peripheral, characteristic: Characteristic, data: ByteArray) {
        peripheral.write(characteristic, data, WriteType.WithResponse)
    }

    // UUIDs based on captured protocol (see provided JS reference files)
    private val NUS_SERVICE_UUID: String = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    private val NUS_RX_CHAR_UUID: String = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
    private val MONITOR_CHAR_UUID: String = "90e991a6-c548-44ed-969b-eb541014eae3"
    private val REP_NOTIFY_CHAR_UUID: String = "8308f2a6-0875-4a94-a86f-5c5c5e1b068a"

    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    private val RX_CHARACTERISTIC = characteristicOf(
        service = NUS_SERVICE_UUID,
        characteristic = NUS_RX_CHAR_UUID
    )

    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    private val MONITOR_CHARACTERISTIC = characteristicOf(
        service = NUS_SERVICE_UUID,
        characteristic = MONITOR_CHAR_UUID
    )

    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    private val REP_NOTIFY_CHARACTERISTIC = characteristicOf(
        service = NUS_SERVICE_UUID,
        characteristic = REP_NOTIFY_CHAR_UUID
    )

    // INIT command (4 bytes)
    private fun buildInitCommand(): ByteArray = byteArrayOf(0x0A, 0x00, 0x00, 0x00)

    // INIT preset (34 bytes)
    private fun buildInitPreset(): ByteArray = byteArrayOf(
        0x11, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0xCD.toByte(), 0xCC.toByte(), 0xCC.toByte(), 0x3E, // 0.4f as little endian
        0xFF.toByte(), 0x00, 0x4C, 0xFF.toByte(), 0x23, 0x8C.toByte(), 0xFF.toByte(), 0x8C.toByte(), 0x8C.toByte(), 0xFF.toByte(),
        0x00, 0x4C, 0xFF.toByte(), 0x23, 0x8C.toByte(), 0xFF.toByte(), 0x8C.toByte(), 0x8C.toByte()
    )

    private fun buildEchoControlFrame(
        level: VitruvianDeviceManager.EchoDifficulty,
        targetReps: Int?,
        eccentricPct: Int = 60, // Reasonable default; real app can surface this later
        gain: Float,
        cap: Float
    ): ByteArray {
        val buf = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)

        // Command ID 0x0000004E
        buf.putInt(0x4E)

        // Warmup reps at 0x04
        buf.put((3).toByte())

        // Target reps at 0x05 (0xFF for Just Lift / unlimited)
        val target = targetReps ?: 0xFF
        buf.put(target.toByte())

        // Reserved u16 at 0x06-0x07
        buf.putShort(0.toShort())

        // Echo params
        // Eccentric % at 0x08 (u16)
        buf.putShort(eccentricPct.toShort())
        // Concentric % at 0x0A (u16) is constant 50
        buf.putShort(50.toShort())
        // Smoothing at 0x0C (f32)
        buf.putFloat(0.1f)

        // Gain at 0x10 (f32)
        buf.putFloat(gain)
        // Cap at 0x14 (f32)
        buf.putFloat(cap)
        // Floor at 0x18 (f32)
        buf.putFloat(0.0f)
        // Neg limit at 0x1C (f32)
        buf.putFloat(-100.0f)

        return buf.array()
    }

    // Parse monitor data bytes to left/right loads in kg
    private fun parseMonitorLoads(data: ByteArray): Pair<Double, Double> {
        if (data.size < 16) return 0.0 to 0.0
        val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        // f4 at offset 0x08 (u16) => loadA (Right)
        val f4 = (bb.getShort(8).toInt() and 0xFFFF)
        // f7 at offset 0x0E (u16) => loadB (Left)
        val f7 = (bb.getShort(14).toInt() and 0xFFFF)
        val rightKg = f4 / 100.0
        val leftKg = f7 / 100.0
        return leftKg to rightKg
    }

    companion object {
        // Positions are u16; values > 50000 are considered invalid spikes per JS reference
        private const val POSITION_SPIKE_FILTER_MAX: Int = 50000

        // Position normalization: map raw position (~millimeters) to 0.0..1.0
        // The web app initializes max at 1000 and adapts dynamically. Based on that and typical
        // Vitruvian travel, we use a conservative fixed divisor of 2000 mm.
        private const val POSITION_NORMALIZATION_DIVISOR_MM: Double = 2000.0

        // Throttle for debug logs (ms)
        private const val POSITION_LOG_THROTTLE_MS: Long = 1000L

        // Movement detection threshold (sum of left+right normalized deltas per tick)
        private const val MOVEMENT_DELTA_THRESHOLD: Double = 0.003

        private const val AUTO_STOP_HOLD_MS: Long = 3_000L
        private const val BOTTOM_POS_THRESHOLD: Double = 0.1
        private const val FORCE_AUTO_STOP_KG: Double = 2.6
        private const val MONITOR_INTERVAL_MS: Long = 100
        private const val CALIBRATION_REPS: Int = 3

        // Pre-initialization timing
        // After prepareForWorkout(), consider the device prepared for this window
        private const val PREPARE_VALID_MS: Long = 10000L

        // Throttle multiple prepare calls to at most once per second
        private const val PREPARE_THROTTLE_MS: Long = 1000L
    }
}
