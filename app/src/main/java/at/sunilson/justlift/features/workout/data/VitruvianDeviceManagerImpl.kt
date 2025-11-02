package at.sunilson.justlift.features.workout.data

import android.content.Context
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
    private val appContext: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : VitruvianDeviceManager {

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

                    // Throttled debug logging for diagnosing position scaling
                    val nowTs = System.currentTimeMillis()
                    if (nowTs - session.lastLogAtMillis > POSITION_LOG_THROTTLE_MS) {
                        Log.d(
                            "VitruvianDevice",
                            "pos raw A=$rawPosA B=$rawPosB | norm R=${String.format("%.3f", posRight)} L=${String.format("%.3f", posLeft)}"
                        )
                        session.lastLogAtMillis = nowTs
                    }

                    // Emit machine state
                    session.machineState.value = VitruvianDeviceManager.MachineState(
                        forceLeftCable = leftKg,
                        forceRightCable = rightKg,
                        positionCableLeft = posLeft,
                        positionCableRight = posRight
                    )

                    // Auto-stop logic: if both cables are near bottom for HOLD then stop
                    val active = session.state.value != null
                    if (active) {
                        val atBottomLeft = posLeft <= BOTTOM_POS_THRESHOLD
                        val atBottomRight = posRight <= BOTTOM_POS_THRESHOLD
                        if (atBottomLeft && atBottomRight) {
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
                delay(100)
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
        // Auto-stop detection
        var bottomHoldSince: Long? = null,
        // Debug logging throttle
        var lastLogAtMillis: Long = 0L,
        // Behavior flags
        var stopAtLastTopRep: Boolean = false,
        // Preparation
        var lastPreparedAtMillis: Long = 0L
    )

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
        device: Peripheral,
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        eccentricPercentage: Double,
        maxReps: Int?,
        stopAtLastTopRep: Boolean
    ) {
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

        // Build Echo control frame using chosen difficulty.
        val frame = buildEchoControlFrame(
            level = difficulty,
            // If maxReps is null we use 0xFF which means unlimited (Just Lift)
            targetReps = maxReps,
            eccentricPct = eccentricPctInt
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
        session.state.value = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = session.calibrationRepsCompleted,
            maxReps = maxReps,
            upwardRepetitionsCompleted = 0,
            downwardRepetitionsCompleted = 0,
            timeElapsed = Duration.ZERO
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

                    // During calibration, perform 3 full reps without counting toward totals
                    if (session.calibrationRepsCompleted < CALIBRATION_REPS) {
                        if (!isUpwardCompletion) {
                            // Count only full reps when bottom is reached
                            session.calibrationRepsCompleted += 1
                            session.state.value = curr.copy(calibratingRepsCompleted = session.calibrationRepsCompleted)
                        }
                        // Do not count reps during calibration
                        return@collect
                    }

                    if (isUpwardCompletion) {
                        // Top reached -> increment upward counter (shown to user)
                        session.upwardReps += 1
                    } else {
                        // Bottom reached -> increment downward counter and check for auto-stop
                        session.downwardReps += 1
                    }

                    val updated = curr.copy(
                        calibratingRepsCompleted = CALIBRATION_REPS,
                        upwardRepetitionsCompleted = session.upwardReps,
                        downwardRepetitionsCompleted = session.downwardReps
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
                            // Default: stop on downward completion when bottom reps reach target
                            if (!isUpwardCompletion && session.downwardReps >= target) {
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
        eccentricPct: Int = 60 // Reasonable default; real app can surface this later
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

        // Level-dependent params
        val (gain, cap) = when (level) {
            VitruvianDeviceManager.EchoDifficulty.HARD -> 1.0f to 50.0f
            VitruvianDeviceManager.EchoDifficulty.HARDER -> 1.25f to 40.0f
            VitruvianDeviceManager.EchoDifficulty.HARDEST -> 1.667f to 30.0f
            VitruvianDeviceManager.EchoDifficulty.EPIC -> 3.333f to 15.0f
        }

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

        private const val AUTO_STOP_HOLD_MS: Long = 5_000L
        private const val BOTTOM_POS_THRESHOLD: Double = 0.05 // 2% of travel considered bottom
        private const val MONITOR_INTERVAL_MS: Long = 100
        private const val CALIBRATION_REPS: Int = 3

        // Pre-initialization timing
        // After prepareForWorkout(), consider the device prepared for this window
        private const val PREPARE_VALID_MS: Long = 10_000L

        // Throttle multiple prepare calls to at most once per second
        private const val PREPARE_THROTTLE_MS: Long = 1_000L
    }
}
