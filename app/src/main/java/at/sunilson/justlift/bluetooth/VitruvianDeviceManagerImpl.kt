package at.sunilson.justlift.bluetooth

import android.content.Context
import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.roundToInt

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
                    val (leftKg, rightKg) = parseMonitorLoads(data)
                    session.machineState.value = VitruvianDeviceManager.MachineState(
                        forceLeftCable = leftKg,
                        forceRightCable = rightKg
                    )
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
        var calibrationRepsToSkip: Int = 3,
        var machineJob: Job? = null,
        var timeJob: Job? = null,
        var repJob: Job? = null
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
        maxReps: Int?
    ) {
        // Send INIT sequence first (per reverse engineered protocol)
        writeWithResponse(device, RX_CHARACTERISTIC, buildInitCommand())
        delay(50)
        writeWithResponse(device, RX_CHARACTERISTIC, buildInitPreset())

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
        val session = sessionFor(device)
        // Cancel any existing jobs for safety
        session.timeJob?.cancel()
        session.repJob?.cancel()
        session.startedAtMillis = System.currentTimeMillis()
        session.maxReps = maxReps
        session.upwardReps = 0
        session.downwardReps = 0
        session.halfRepNotifications = 0
        session.calibrationRepsToSkip = 3
        session.state.value = VitruvianDeviceManager.WorkoutState(
            calibrating = true,
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

                    // During calibration, ignore first 3 full reps (i.e., first 6 half-reps)
                    if (session.calibrationRepsToSkip > 0) {
                        // Only decrement calibration counter when a full rep (downward completion) occurs
                        if (!isUpwardCompletion) {
                            session.calibrationRepsToSkip -= 1
                        }
                        val stillCalibrating = session.calibrationRepsToSkip > 0
                        session.state.value = curr.copy(calibrating = stillCalibrating)
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
                        calibrating = false,
                        upwardRepetitionsCompleted = session.upwardReps,
                        downwardRepetitionsCompleted = session.downwardReps
                    )
                    session.state.value = updated

                    // Stop only when downward reps reach target
                    val target = session.maxReps
                    if (target != null && session.downwardReps >= target && !isUpwardCompletion) {
                        stopWorkout(device)
                    }
                }
            } catch (_: Throwable) {
                // Observation ends
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
}
