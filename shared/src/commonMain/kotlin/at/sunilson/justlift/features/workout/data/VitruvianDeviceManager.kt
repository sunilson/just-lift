package at.sunilson.justlift.features.workout.data

import com.juul.kable.Peripheral
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface VitruvianDeviceManager {

    /**
     * Scan devices that start name with "Vee"
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
     * @param maxReps Optional maxReps to set for the workout. Reaching this limit will trigger a sound in the app but will not stop the workout automatically.
     * @param eccentricPercentage Percentage of the repetition time that should be spent in the eccentric phase (0.0 - 1.3)
     */
    suspend fun startWorkout(
        userId: Int,
        device: Peripheral,
        difficulty: EchoDifficulty,
        eccentricPercentage: Double,
        maxReps: Int? = null,
        stopAtLastTopRep: Boolean = false
    )

    /**
     * Starts a fixed-weight (Old School) workout with the given weight per cable.
     *
     * @param weightPerCableKg Weight in kg per cable (5-100)
     * @param eccentricPercentage Percentage of the repetition time that should be spent in the eccentric phase (0.0 - 1.3)
     * @param maxReps Optional maxReps to set for the workout.
     */
    suspend fun startFixedWeightWorkout(
        userId: Int,
        device: Peripheral,
        weightPerCableKg: Float,
        eccentricPercentage: Double,
        maxReps: Int? = null
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
        val maxDownwardForce: Double = 0.0,
        val averageUpwardForceLeft: Double = 0.0,
        val averageUpwardForceRight: Double = 0.0,
        val averageDownwardForceLeft: Double = 0.0,
        val averageDownwardForceRight: Double = 0.0,
        val minPositionLeft: Double = 0.0,
        val maxPositionLeft: Double = 0.0,
        val minPositionRight: Double = 0.0,
        val maxPositionRight: Double = 0.0,
        val avgMinPositionLeft: Double = 0.0,
        val avgMaxPositionLeft: Double = 0.0,
        val avgMinPositionRight: Double = 0.0,
        val avgMaxPositionRight: Double = 0.0,
        val avgUpwardRepDurationMillis: Double = 0.0,
        val avgDownwardRepDurationMillis: Double = 0.0,
        val avgUpwardPeakForcePosition: Double = 0.0,
        val avgDownwardPeakForcePosition: Double = 0.0,
        val avgUpwardMaxVelocity: Double = 0.0,
        val avgDownwardMaxVelocity: Double = 0.0,
        val avgRestDurationMillis: Double = 0.0,
        val difficulty: EchoDifficulty = EchoDifficulty.WARMUP
    )

    data class MachineState(
        val forceLeftCable: Double,
        val forceRightCable: Double,
        // Normalized positions in range 0.0..1.0
        val positionCableLeft: Double,
        val positionCableRight: Double
    )

    enum class EchoDifficulty {
        WARMUP, HARD, HARDER, HARDEST //, EPIC
    }
}
