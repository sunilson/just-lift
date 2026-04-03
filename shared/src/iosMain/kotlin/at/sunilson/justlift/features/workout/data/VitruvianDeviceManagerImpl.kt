@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package at.sunilson.justlift.features.workout.data

import at.sunilson.justlift.platform.currentTimeMillis
import at.sunilson.justlift.platform.platformLog
import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

                    val (leftKg, rightKg) = parseMonitorLoads(data)

                    var rawPosA = if (data.size >= 12) readUInt16LE(data, 4) else 0
                    var rawPosB = if (data.size >= 16) readUInt16LE(data, 10) else 0
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

                    var deltaL = 0.0
                    var deltaR = 0.0
                    val moving: Boolean = if (!session.prevPosInitialized) {
                        session.prevPosInitialized = true
                        session.prevPosLeft = posLeft
                        session.prevPosRight = posRight
                        false
                    } else {
                        deltaL = abs(posLeft - session.prevPosLeft)
                        deltaR = abs(posRight - session.prevPosRight)
                        session.prevPosLeft = posLeft
                        session.prevPosRight = posRight
                        (deltaL + deltaR) > MOVEMENT_DELTA_THRESHOLD
                    }

                    val velocity = (deltaL + deltaR) / (MONITOR_INTERVAL_MS / 1000.0)
                    if (velocity > session.repMaxVelocity) session.repMaxVelocity = velocity

                    session.machineState.value = VitruvianDeviceManager.MachineState(
                        forceLeftCable = leftKg,
                        forceRightCable = rightKg,
                        positionCableLeft = posLeft,
                        positionCableRight = posRight
                    )

                    val activeState = session.state.value
                    if (activeState != null && session.calibrationRepsCompleted >= CALIBRATION_REPS) {
                        val phase = session.currentPhase
                        if (phase != null) {
                            val combined = (leftKg + rightKg) / 2.0
                            if (moving) {
                                if (session.repStartedMovingAt == null) {
                                    val now = currentTimeMillis()
                                    session.repStartedMovingAt = now
                                    session.lastRepNotificationAtMillis?.let { lastNotif ->
                                        session.avgRestDurationSum += (now - lastNotif)
                                        session.avgRestDurationCount += 1
                                    }
                                }

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

                    val active = session.state.value != null
                    if (active) {
                        val atBottomLeft = posLeft <= BOTTOM_POS_THRESHOLD
                        val atBottomRight = posRight <= BOTTOM_POS_THRESHOLD
                        val isCalibrating = session.calibrationRepsCompleted < CALIBRATION_REPS
                        val lightLoadBoth = !isCalibrating && (leftKg <= FORCE_AUTO_STOP_KG && rightKg <= FORCE_AUTO_STOP_KG)
                        val shouldAutoStop = (atBottomLeft && atBottomRight) || lightLoadBoth

                        if (shouldAutoStop) {
                            val now = currentTimeMillis()
                            if (session.bottomHoldSince == null) {
                                session.bottomHoldSince = now
                            }
                            val elapsed = now - (session.bottomHoldSince ?: now)
                            val remainingMs = AUTO_STOP_HOLD_MS - elapsed
                            val secondsLeft = if (remainingMs > 0) ceil(remainingMs / 1000.0).toInt() else 0

                            session.state.value = session.state.value?.copy(autoStopInSeconds = secondsLeft)

                            if (elapsed >= AUTO_STOP_HOLD_MS) {
                                session.bottomHoldSince = null
                                try { stopWorkout(device) } catch (_: Throwable) {}
                            }
                        } else {
                            session.bottomHoldSince = null
                            if (session.state.value?.autoStopInSeconds != null) {
                                session.state.value = session.state.value?.copy(autoStopInSeconds = null)
                            }
                        }
                    } else {
                        if (session.state.value?.autoStopInSeconds != null) {
                            session.state.value = session.state.value?.copy(autoStopInSeconds = null)
                        }
                        session.bottomHoldSince = null
                    }
                } catch (_: Throwable) {
                    // Likely disconnected or transient read failure
                }
                delay(MONITOR_INTERVAL_MS)
            }
        }
    }

    private data class WorkoutSession(
        var startedAtMillis: Long = 0L,
        var maxReps: Int? = null,
        val state: MutableStateFlow<VitruvianDeviceManager.WorkoutState?> = MutableStateFlow(null),
        val machineState: MutableStateFlow<VitruvianDeviceManager.MachineState?> = MutableStateFlow(null),
        var upwardReps: Int = 0,
        var downwardReps: Int = 0,
        var halfRepNotifications: Int = 0,
        var calibrationRepsCompleted: Int = 0,
        var machineJob: Job? = null,
        var timeJob: Job? = null,
        var repJob: Job? = null,
        var lastGoodPosA: Int = 0,
        var lastGoodPosB: Int = 0,
        var prevPosLeft: Double = 0.0,
        var prevPosRight: Double = 0.0,
        var prevPosInitialized: Boolean = false,
        var bottomHoldSince: Long? = null,
        var stopAtLastTopRep: Boolean = false,
        var lastPreparedAtMillis: Long = 0L,
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
        sessions.getOrPut(device.identifier.toString()) { WorkoutSession() }

    override fun getScannedDevicesFlow(): Flow<List<Peripheral>> {
        return try {
            Scanner().advertisements
                .mapNotNull { adv: Advertisement ->
                    if (adv.name?.startsWith("Vee") == true) {
                        Peripheral(adv)
                    } else null
                }
                .runningFold(mapOf<String, Peripheral>()) { acc, peripheral -> acc + (peripheral.identifier.toString() to peripheral) }
                .mapNotNull { it.values.toList() }
                .catch { e ->
                    platformLog("VitruvianDeviceManager", "BLE scanning error: ${e.message}")
                    emit(emptyList())
                }
        } catch (e: Exception) {
            platformLog("VitruvianDeviceManager", "BLE not available: ${e.message}")
            emptyFlow()
        }
    }

    override fun getWorkoutStateFlow(device: Peripheral): Flow<VitruvianDeviceManager.WorkoutState?> {
        return sessionFor(device).state
    }

    override fun getMachineStateFlow(device: Peripheral): Flow<VitruvianDeviceManager.MachineState?> {
        val session = sessionFor(device)
        ensureMachineJobRunning(device)
        return session.machineState
    }

    private suspend fun prepareAndSendFrame(device: Peripheral, session: WorkoutSession, frame: ByteArray) {
        val now = currentTimeMillis()
        if (now - session.lastPreparedAtMillis > PREPARE_VALID_MS) {
            writeWithResponse(device, RX_CHARACTERISTIC, buildInitCommand())
            delay(50)
            writeWithResponse(device, RX_CHARACTERISTIC, buildInitPreset())
            session.lastPreparedAtMillis = now
        }
        writeWithResponse(device, RX_CHARACTERISTIC, frame)
    }

    override suspend fun startWorkout(
        userId: Int,
        device: Peripheral,
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        eccentricPercentage: Double,
        maxReps: Int?,
        stopAtLastTopRep: Boolean
    ) {
        platformLog("VitruvianDeviceManager", "startWorkout: userId=$userId, difficulty=$difficulty, eccentric=$eccentricPercentage, maxReps=$maxReps")
        val session = sessionFor(device)

        val eccentricRatio = eccentricPercentage.coerceIn(0.0, 1.3)
        val eccentricPctInt = (eccentricRatio * 100.0).roundToInt().coerceIn(0, 130)

        val modeParams = workoutSettingsRepository.getModeParameters(userId, difficulty)

        val frame = buildEchoControlFrame(
            level = difficulty,
            targetReps = null,
            eccentricPct = eccentricPctInt,
            gain = modeParams.gain,
            cap = modeParams.capKg
        )
        prepareAndSendFrame(device, session, frame)

        resetSession(session, maxReps, stopAtLastTopRep, difficulty)
        session.state.value = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = 0,
            maxReps = maxReps,
            upwardRepetitionsCompleted = 0,
            downwardRepetitionsCompleted = 0,
            timeElapsed = Duration.ZERO,
            difficulty = difficulty
        )

        ensureMachineJobRunning(device)
        startTimeAndRepJobs(device, session)
    }

    override suspend fun startFixedWeightWorkout(
        userId: Int,
        device: Peripheral,
        weightPerCableKg: Float,
        eccentricPercentage: Double,
        maxReps: Int?
    ) {
        platformLog("VitruvianDeviceManager", "startFixedWeightWorkout: userId=$userId, weight=${weightPerCableKg}kg")
        val session = sessionFor(device)

        val frame = buildOldSchoolStartCommand(weightPerCableKg)
        prepareAndSendFrame(device, session, frame)

        resetSession(session, maxReps, stopAtLastTopRep = false, VitruvianDeviceManager.EchoDifficulty.WARMUP)
        session.state.value = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = 0,
            maxReps = maxReps,
            upwardRepetitionsCompleted = 0,
            downwardRepetitionsCompleted = 0,
            timeElapsed = Duration.ZERO,
            difficulty = VitruvianDeviceManager.EchoDifficulty.WARMUP
        )

        ensureMachineJobRunning(device)
        startTimeAndRepJobs(device, session)
    }

    private fun resetSession(
        session: WorkoutSession,
        maxReps: Int?,
        stopAtLastTopRep: Boolean,
        difficulty: VitruvianDeviceManager.EchoDifficulty
    ) {
        session.timeJob?.cancel()
        session.repJob?.cancel()
        session.startedAtMillis = currentTimeMillis()
        session.maxReps = maxReps
        session.upwardReps = 0
        session.downwardReps = 0
        session.halfRepNotifications = 0
        session.calibrationRepsCompleted = 0
        session.stopAtLastTopRep = stopAtLastTopRep
        session.upForceSum = 0.0; session.upForceCount = 0
        session.downForceSum = 0.0; session.downForceCount = 0
        session.maxUpForce = 0.0; session.maxDownForce = 0.0
        session.upForceLeftSum = 0.0; session.upForceRightSum = 0.0
        session.downForceLeftSum = 0.0; session.downForceRightSum = 0.0
        session.minPosLeft = 1.0; session.maxPosLeft = 0.0
        session.minPosRight = 1.0; session.maxPosRight = 0.0
        session.minPosLeftSum = 0.0; session.minPosLeftCount = 0
        session.maxPosLeftSum = 0.0; session.maxPosLeftCount = 0
        session.minPosRightSum = 0.0; session.minPosRightCount = 0
        session.maxPosRightSum = 0.0; session.maxPosRightCount = 0
        session.lastRepNotificationAtMillis = null
        session.upwardDurationSum = 0; session.upwardDurationCount = 0
        session.downwardDurationSum = 0; session.downwardDurationCount = 0
        session.avgUpwardPeakForcePosSum = 0.0; session.avgUpwardPeakForcePosCount = 0
        session.avgDownwardPeakForcePosSum = 0.0; session.avgDownwardPeakForcePosCount = 0
        session.avgUpwardMaxVelocitySum = 0.0; session.avgUpwardMaxVelocityCount = 0
        session.avgDownwardMaxVelocitySum = 0.0; session.avgDownwardMaxVelocityCount = 0
        session.avgRestDurationSum = 0; session.avgRestDurationCount = 0
        session.repPeakForce = 0.0; session.repPeakForcePos = 0.0
        session.repMaxVelocity = 0.0; session.repStartedMovingAt = null
        session.currentPhase = null; session.difficulty = difficulty
        session.prevPosInitialized = false; session.prevPosLeft = 0.0; session.prevPosRight = 0.0
    }

    private fun startTimeAndRepJobs(device: Peripheral, session: WorkoutSession) {
        session.timeJob = scope.launch {
            while (isActive) {
                val elapsed = (currentTimeMillis() - session.startedAtMillis).milliseconds
                val curr = session.state.value ?: break
                session.state.value = curr.copy(timeElapsed = elapsed)
                delay(100)
            }
        }

        session.repJob = scope.launch {
            try {
                device.observe(REP_NOTIFY_CHARACTERISTIC).collect {
                    val curr = session.state.value ?: return@collect

                    session.halfRepNotifications += 1
                    val isUpwardCompletion = (session.halfRepNotifications % 2 == 1)

                    val now = currentTimeMillis()
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

                    if (session.calibrationRepsCompleted < CALIBRATION_REPS) {
                        if (isUpwardCompletion) {
                            session.calibrationRepsCompleted += 1
                            session.state.value = curr.copy(calibratingRepsCompleted = session.calibrationRepsCompleted)
                            if (session.calibrationRepsCompleted >= CALIBRATION_REPS) {
                                session.currentPhase = Phase.DOWN
                            }
                        }
                        return@collect
                    }

                    if (isUpwardCompletion) {
                        if (session.repPeakForce > 0) {
                            session.avgUpwardPeakForcePosSum += session.repPeakForcePos
                            session.avgUpwardPeakForcePosCount += 1
                        }
                        session.avgUpwardMaxVelocitySum += session.repMaxVelocity
                        session.avgUpwardMaxVelocityCount += 1
                        session.upwardReps += 1
                        session.currentPhase = Phase.DOWN
                    } else {
                        if (session.repPeakForce > 0) {
                            session.avgDownwardPeakForcePosSum += session.repPeakForcePos
                            session.avgDownwardPeakForcePosCount += 1
                        }
                        session.avgDownwardMaxVelocitySum += session.repMaxVelocity
                        session.avgDownwardMaxVelocityCount += 1
                        session.downwardReps += 1
                        session.currentPhase = Phase.UP
                    }

                    session.repPeakForce = 0.0
                    session.repPeakForcePos = 0.0
                    session.repMaxVelocity = 0.0
                    session.repStartedMovingAt = null

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
                }
            } catch (_: Throwable) {}
        }
    }

    override suspend fun prepareForWorkout(device: Peripheral) {
        val session = sessionFor(device)
        val now = currentTimeMillis()
        if (now - session.lastPreparedAtMillis > PREPARE_THROTTLE_MS) {
            try {
                writeWithResponse(device, RX_CHARACTERISTIC, buildInitCommand())
                delay(50)
                writeWithResponse(device, RX_CHARACTERISTIC, buildInitPreset())
                session.lastPreparedAtMillis = now
            } catch (_: Throwable) {}
        }
    }

    override suspend fun stopWorkout(device: Peripheral) {
        platformLog("VitruvianDeviceManager", "stopWorkout: device=${device.identifier}")
        try {
            writeWithResponse(device, RX_CHARACTERISTIC, buildInitCommand())
        } catch (_: Throwable) {}
        val session = sessionFor(device)
        session.timeJob?.cancel()
        session.repJob?.cancel()
        session.timeJob = null
        session.repJob = null
        session.state.value = null
        ensureMachineJobRunning(device)
    }

    // --- BLE + Protocol helpers -----------------------------------------------------------------

    private suspend fun writeWithResponse(peripheral: Peripheral, characteristic: Characteristic, data: ByteArray) {
        peripheral.write(characteristic, data, WriteType.WithResponse)
    }

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

    // --- Byte manipulation (replacing java.nio.ByteBuffer) ---

    private fun readUInt16LE(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun writeInt32LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeInt16LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun writeFloat32LE(buf: ByteArray, offset: Int, value: Float) {
        writeInt32LE(buf, offset, value.toRawBits())
    }

    private fun buildInitCommand(): ByteArray = byteArrayOf(0x0A, 0x00, 0x00, 0x00)

    private fun buildInitPreset(): ByteArray = byteArrayOf(
        0x11, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0xCD.toByte(), 0xCC.toByte(), 0xCC.toByte(), 0x3E,
        0xFF.toByte(), 0x00, 0x4C, 0xFF.toByte(), 0x23, 0x8C.toByte(), 0xFF.toByte(), 0x8C.toByte(), 0x8C.toByte(), 0xFF.toByte(),
        0x00, 0x4C, 0xFF.toByte(), 0x23, 0x8C.toByte(), 0xFF.toByte(), 0x8C.toByte(), 0x8C.toByte()
    )

    private fun buildEchoControlFrame(
        level: VitruvianDeviceManager.EchoDifficulty,
        targetReps: Int?,
        eccentricPct: Int = 60,
        gain: Float,
        cap: Float
    ): ByteArray {
        val buf = ByteArray(32)

        // Command ID 0x0000004E
        writeInt32LE(buf, 0, 0x4E)
        // Warmup reps at 0x04
        buf[4] = 3.toByte()
        // Target reps at 0x05 (0xFF for unlimited)
        buf[5] = (targetReps ?: 0xFF).toByte()
        // Reserved u16 at 0x06-0x07
        writeInt16LE(buf, 6, 0)
        // Eccentric % at 0x08 (u16)
        writeInt16LE(buf, 8, eccentricPct)
        // Concentric % at 0x0A (u16)
        writeInt16LE(buf, 10, 50)
        // Smoothing at 0x0C (f32)
        writeFloat32LE(buf, 12, 0.1f)
        // Gain at 0x10 (f32)
        writeFloat32LE(buf, 16, gain)
        // Cap at 0x14 (f32)
        writeFloat32LE(buf, 20, cap)
        // Floor at 0x18 (f32)
        writeFloat32LE(buf, 24, 0.0f)
        // Neg limit at 0x1C (f32)
        writeFloat32LE(buf, 28, -100.0f)

        return buf
    }

    private fun buildOldSchoolStartCommand(weightPerCableKg: Float): ByteArray {
        val weightEncoded = (weightPerCableKg * 100).toInt().coerceIn(0, 0xFFFF)
        val weightLow = (weightEncoded and 0xFF).toByte()
        val weightHigh = ((weightEncoded shr 8) and 0xFF).toByte()
        return byteArrayOf(0x02, 0x00, weightLow, weightHigh)
    }

    private fun parseMonitorLoads(data: ByteArray): Pair<Double, Double> {
        if (data.size < 16) return 0.0 to 0.0
        val f4 = readUInt16LE(data, 8)  // loadA (Right)
        val f7 = readUInt16LE(data, 14) // loadB (Left)
        val rightKg = f4 / 100.0
        val leftKg = f7 / 100.0
        return leftKg to rightKg
    }

    companion object {
        private const val POSITION_SPIKE_FILTER_MAX: Int = 50000
        private const val POSITION_NORMALIZATION_DIVISOR_MM: Double = 2000.0
        private const val MOVEMENT_DELTA_THRESHOLD: Double = 0.003
        private const val AUTO_STOP_HOLD_MS: Long = 3_000L
        private const val BOTTOM_POS_THRESHOLD: Double = 0.1
        private const val FORCE_AUTO_STOP_KG: Double = 2.6
        private const val MONITOR_INTERVAL_MS: Long = 100
        private const val CALIBRATION_REPS: Int = 3
        private const val PREPARE_VALID_MS: Long = 10000L
        private const val PREPARE_THROTTLE_MS: Long = 1000L
    }
}
