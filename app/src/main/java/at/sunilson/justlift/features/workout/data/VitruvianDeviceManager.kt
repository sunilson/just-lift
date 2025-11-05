package at.sunilson.justlift.features.workout.data

import androidx.annotation.FloatRange
import com.juul.kable.Peripheral
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface VitruvianDeviceManager {

    /**
     * Scan devices that start name with "Vee
     */
    fun getScannedDevicesFlow(): Flow<List<Peripheral>>

    /**
     * Provides a flow of the current workout state for the given device. Null if no workout is active.
     */
    fun getWorkoutStateFlow(device: Peripheral): Flow<WorkoutState?>

    /**
     * Provides a flow of the current machine state (cable forces for example) for the given device.
     * This should emit regardless of workout activity, enabling auto-detection of workout start/end.
     */
    fun getMachineStateFlow(device: Peripheral): Flow<MachineState?>

    /**
     * Optionally pre-initialize the device (send INIT and PRESET) to reduce the latency of a later start.
     * Implementations should internally throttle this call.
     */
    suspend fun prepareForWorkout(device: Peripheral)

    /**
     * Starts a just lift workout in echo mode with the given difficulty
     *
     * @param maxReps Optional maxReps to set for the workout, after which the workout will automatically stop
     * @param eccentricPercentage Percentage of the repetition time that should be spent in the eccentric phase (0.0 - 1.3)
     */
    suspend fun startWorkout(
        device: Peripheral,
        difficulty: EchoDifficulty,
        @FloatRange(0.0, 1.3)
        eccentricPercentage: Double,
        maxReps: Int? = null,
        stopAtLastTopRep: Boolean = false
    )

    /**
     * Stops the current workout on the given device
     */
    suspend fun stopWorkout(device: Peripheral)

    data class WorkoutState(
        val calibratingRepsCompleted: Int,
        val maxReps: Int?,
        val upwardRepetitionsCompleted: Int,
        val downwardRepetitionsCompleted: Int,
        val timeElapsed: Duration,
        val autoStopInSeconds: Int? = null,
        val averageUpwardForce: Double = 0.0,
        val averageDownwardForce: Double = 0.0,
        val maxUpwardForce: Double = 0.0,
        val maxDownwardForce: Double = 0.0
    )

    data class MachineState(
        val forceLeftCable: Double,
        val forceRightCable: Double,
        // Normalized positions in range 0.0..1.0
        val positionCableLeft: Double,
        val positionCableRight: Double
    )

    enum class EchoDifficulty {
        HARD, HARDER, HARDEST, EPIC
    }
}
