package at.sunilson.justlift.features.workout.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for persisting and restoring workout defaults.
 * Stores:
 * - echo difficulty
 * - repetitions value
 * - eccentric percentage (0f..130f as UI uses percent)
 */
interface WorkoutSettingsRepository {
    fun settingsFlow(userId: Int): Flow<WorkoutSettings>
    suspend fun get(userId: Int): WorkoutSettings
    suspend fun save(userId: Int, settings: WorkoutSettings)

    // Per-difficulty settings
    suspend fun getDifficultySettings(userId: Int, difficulty: VitruvianDeviceManager.EchoDifficulty): DifficultySettings
    suspend fun saveDifficultySettings(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        settings: DifficultySettings
    )
    suspend fun resetDifficultySettings(userId: Int, difficulty: VitruvianDeviceManager.EchoDifficulty)

    // Per-difficulty machine parameters (used by Echo control frame)
    suspend fun getModeParameters(userId: Int, difficulty: VitruvianDeviceManager.EchoDifficulty): ModeParameters
    suspend fun saveModeParameters(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        params: ModeParameters
    )
    suspend fun resetModeParameters(userId: Int, difficulty: VitruvianDeviceManager.EchoDifficulty)

    // Saved device (for auto-connect)
    val savedDeviceFlow: Flow<SavedDevice?>
    suspend fun setLastDevice(id: String, name: String?)
    suspend fun clearLastDevice()
}

data class WorkoutSettings(
    val echoDifficulty: VitruvianDeviceManager.EchoDifficulty = VitruvianDeviceManager.EchoDifficulty.WARMUP,
    val repetitions: Int = 8,
    val eccentricPercentage: Float = 100f,
    val useTts: Boolean = false,
    val fixedWeightMode: Boolean = false,
    val fixedWeightKg: Float = 20f
)

data class DifficultySettings(
    val repetitions: Int = 8,
    val eccentricPercentage: Float = 100f
)

/**
 * Per-mode machine parameters for Echo control frame.
 */
data class ModeParameters(
    val gain: Float,
    val capKg: Float
)

data class SavedDevice(
    val id: String,
    val name: String?
)
