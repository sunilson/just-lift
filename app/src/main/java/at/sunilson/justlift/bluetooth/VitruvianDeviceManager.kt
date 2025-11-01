package at.sunilson.justlift.bluetooth

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
     * Starts a just lift workout in echo mode with the given difficulty
     *
     * @param maxReps Optional maxReps to set for the workout, after which the workout will automatically stop
     * @param eccentricPercentage Percentage of the repetition time that should be spent in the eccentric phase (0.0 - 1.3)
     */
    suspend fun startJustLiftEchoWorkout(
        device: Peripheral,
        difficulty: EchoDifficulty,
        @FloatRange(0.0, 1.3)
        eccentricPercentage: Double,
        maxReps: Int? = null
    )

    /**
     * Stops the current workout on the given device
     */
    suspend fun stopWorkout(device: Peripheral)

    data class WorkoutState(
        val calibrating: Boolean,
        val maxReps: Int?,
        val upwardRepetitionsCompleted: Int,
        val downwardRepetitionsCompleted: Int,
        val forceLeftCable: Double,
        val forceRightCable: Double,
        val timeElapsed: Duration
    )

    enum class EchoDifficulty {
        HARD, HARDER, HARDEST, EPIC
    }
}
