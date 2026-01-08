package at.sunilson.justlift.features.workout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import java.io.IOException

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
    val eccentricPercentage: Float = 100f
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

@Single(binds = [WorkoutSettingsRepository::class])
class WorkoutSettingsRepositoryImpl(
    context: Context
) : WorkoutSettingsRepository {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATASTORE_FILE) }
    )

    override fun settingsFlow(userId: Int): Flow<WorkoutSettings> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            WorkoutSettings(
                echoDifficulty = prefs[stringPreferencesKey(userKey(KEY_DIFFICULTY_NAME, userId))]?.let { name ->
                    runCatching { VitruvianDeviceManager.EchoDifficulty.valueOf(name) }
                        .getOrDefault(WorkoutSettings().echoDifficulty)
                } ?: WorkoutSettings().echoDifficulty,
                repetitions = prefs[intPreferencesKey(userKey(KEY_REPS_NAME, userId))]
                    ?: WorkoutSettings().repetitions,
                eccentricPercentage = prefs[floatPreferencesKey(userKey(KEY_ECCENTRIC_PERCENT_NAME, userId))]
                    ?: WorkoutSettings().eccentricPercentage
            )
        }

    override suspend fun get(userId: Int): WorkoutSettings = settingsFlow(userId).first()

    override suspend fun save(userId: Int, settings: WorkoutSettings) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(userKey(KEY_DIFFICULTY_NAME, userId))] = settings.echoDifficulty.name
            prefs[intPreferencesKey(userKey(KEY_REPS_NAME, userId))] = settings.repetitions
            prefs[floatPreferencesKey(userKey(KEY_ECCENTRIC_PERCENT_NAME, userId))] = settings.eccentricPercentage
        }
    }

    override suspend fun getDifficultySettings(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty
    ): DifficultySettings {
        // Read difficulty-specific values from store; fall back to global defaults if missing
        val prefs = dataStore.data.first()
        return DifficultySettings(
            repetitions = prefs[intKey(diffKey(KEY_REPS_NAME, difficulty, userId))]
                ?: (prefs[intPreferencesKey(userKey(KEY_REPS_NAME, userId))]
                    ?: DifficultySettings().repetitions),
            eccentricPercentage = prefs[floatKey(diffKey(KEY_ECCENTRIC_PERCENT_NAME, difficulty, userId))]
                ?: (prefs[floatPreferencesKey(userKey(KEY_ECCENTRIC_PERCENT_NAME, userId))]
                    ?: DifficultySettings().eccentricPercentage)
        )
    }

    override suspend fun saveDifficultySettings(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        settings: DifficultySettings
    ) {
        dataStore.edit { prefs ->
            prefs[intKey(diffKey(KEY_REPS_NAME, difficulty, userId))] = settings.repetitions
            prefs[floatKey(diffKey(KEY_ECCENTRIC_PERCENT_NAME, difficulty, userId))] = settings.eccentricPercentage
        }
    }

    override suspend fun resetDifficultySettings(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty
    ) {
        dataStore.edit { prefs ->
            prefs.remove(intKey(diffKey(KEY_REPS_NAME, difficulty, userId)))
            prefs.remove(floatKey(diffKey(KEY_ECCENTRIC_PERCENT_NAME, difficulty, userId)))
        }
    }

    override suspend fun getModeParameters(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty
    ): ModeParameters {
        val prefs = dataStore.data.first()
        val defaults = defaultModeParameters(difficulty)
        val gainKey = floatKey(modeKey("mode_gain", difficulty, userId))
        val capKey = floatKey(modeKey("mode_cap", difficulty, userId))
        return ModeParameters(
            gain = prefs[gainKey] ?: defaults.gain,
            capKg = prefs[capKey] ?: defaults.capKg
        )
    }

    override suspend fun saveModeParameters(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        params: ModeParameters
    ) {
        dataStore.edit { prefs ->
            prefs[floatKey(modeKey("mode_gain", difficulty, userId))] = params.gain
            prefs[floatKey(modeKey("mode_cap", difficulty, userId))] = params.capKg
        }
    }

    override suspend fun resetModeParameters(
        userId: Int,
        difficulty: VitruvianDeviceManager.EchoDifficulty
    ) {
        dataStore.edit { prefs ->
            prefs.remove(floatKey(modeKey("mode_gain", difficulty, userId)))
            prefs.remove(floatKey(modeKey("mode_cap", difficulty, userId)))
        }
    }

    override val savedDeviceFlow: Flow<SavedDevice?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val id = prefs[KEY_LAST_DEVICE_ID]
            if (id.isNullOrEmpty()) null else SavedDevice(id = id, name = prefs[KEY_LAST_DEVICE_NAME])
        }

    override suspend fun setLastDevice(id: String, name: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_DEVICE_ID] = id
            if (name != null) {
                prefs[KEY_LAST_DEVICE_NAME] = name
            } else {
                prefs.remove(KEY_LAST_DEVICE_NAME)
            }
        }
    }

    override suspend fun clearLastDevice() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_DEVICE_ID)
            prefs.remove(KEY_LAST_DEVICE_NAME)
        }
    }

    private companion object {
        private const val DATASTORE_FILE = "workout_settings.preferences_pb"

        private const val KEY_DIFFICULTY_NAME = "difficulty"
        private const val KEY_REPS_NAME = "reps"
        private const val KEY_ECCENTRIC_PERCENT_NAME = "eccentric_percent"

        private val KEY_LAST_DEVICE_ID = stringPreferencesKey("last_device_id")
        private val KEY_LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")

        private fun userKey(base: String, userId: Int): String = "${base}_u${userId}"

        // Helpers to create typed keys dynamically for per-difficulty settings
        private fun diffKey(base: String, difficulty: VitruvianDeviceManager.EchoDifficulty, userId: Int): String =
            "${base}_${difficulty.name.lowercase()}_u${userId}"

        private fun modeKey(base: String, difficulty: VitruvianDeviceManager.EchoDifficulty, userId: Int): String =
            "${base}_${difficulty.name.lowercase()}_u${userId}"

        private fun boolKey(name: String) = booleanPreferencesKey(name)
        private fun intKey(name: String) = intPreferencesKey(name)
        private fun floatKey(name: String) = floatPreferencesKey(name)

        private fun defaultModeParameters(d: VitruvianDeviceManager.EchoDifficulty): ModeParameters = when (d) {
            VitruvianDeviceManager.EchoDifficulty.WARMUP -> ModeParameters(gain = 0.5f, capKg = 50.0f)
            VitruvianDeviceManager.EchoDifficulty.HARD -> ModeParameters(gain = 1.0f, capKg = 50.0f)
            VitruvianDeviceManager.EchoDifficulty.HARDER -> ModeParameters(gain = 1.25f, capKg = 40.0f)
            VitruvianDeviceManager.EchoDifficulty.HARDEST -> ModeParameters(gain = 1.667f, capKg = 30.0f)
            // VitruvianDeviceManager.EchoDifficulty.EPIC -> ModeParameters(gain = 3.333f, capKg = 15.0f)
        }
    }
}
