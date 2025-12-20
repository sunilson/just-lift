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
 * - useNoRepLimit flag and repetitions value
 * - eccentric percentage (0f..130f as UI uses percent)
 */
interface WorkoutSettingsRepository {
    val settingsFlow: Flow<WorkoutSettings>
    suspend fun get(): WorkoutSettings
    suspend fun save(settings: WorkoutSettings)

    // Per-difficulty settings
    suspend fun getDifficultySettings(difficulty: VitruvianDeviceManager.EchoDifficulty): DifficultySettings
    suspend fun saveDifficultySettings(
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        settings: DifficultySettings
    )
    suspend fun resetDifficultySettings(difficulty: VitruvianDeviceManager.EchoDifficulty)

    // Per-difficulty machine parameters (used by Echo control frame)
    suspend fun getModeParameters(difficulty: VitruvianDeviceManager.EchoDifficulty): ModeParameters
    suspend fun saveModeParameters(
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        params: ModeParameters
    )
    suspend fun resetModeParameters(difficulty: VitruvianDeviceManager.EchoDifficulty)

    // Saved device (for auto-connect)
    val savedDeviceFlow: Flow<SavedDevice?>
    suspend fun setLastDevice(id: String, name: String?)
    suspend fun clearLastDevice()
}

data class WorkoutSettings(
    val echoDifficulty: VitruvianDeviceManager.EchoDifficulty = VitruvianDeviceManager.EchoDifficulty.HARDEST,
    val useNoRepLimit: Boolean = true,
    val repetitions: Int = 8,
    val eccentricPercentage: Float = 100f
)

data class DifficultySettings(
    val useNoRepLimit: Boolean = true,
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

    override val settingsFlow: Flow<WorkoutSettings> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            WorkoutSettings(
                echoDifficulty = prefs[KEY_DIFFICULTY]?.let { name ->
                    runCatching { VitruvianDeviceManager.EchoDifficulty.valueOf(name) }
                        .getOrDefault(WorkoutSettings().echoDifficulty)
                } ?: WorkoutSettings().echoDifficulty,
                useNoRepLimit = prefs[KEY_USE_NO_REP] ?: WorkoutSettings().useNoRepLimit,
                repetitions = prefs[KEY_REPS] ?: WorkoutSettings().repetitions,
                eccentricPercentage = prefs[KEY_ECCENTRIC_PERCENT] ?: WorkoutSettings().eccentricPercentage
            )
        }

    override suspend fun get(): WorkoutSettings = settingsFlow.first()

    override suspend fun save(settings: WorkoutSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_DIFFICULTY] = settings.echoDifficulty.name
            prefs[KEY_USE_NO_REP] = settings.useNoRepLimit
            prefs[KEY_REPS] = settings.repetitions
            prefs[KEY_ECCENTRIC_PERCENT] = settings.eccentricPercentage
        }
    }

    override suspend fun getDifficultySettings(difficulty: VitruvianDeviceManager.EchoDifficulty): DifficultySettings {
        // Read difficulty-specific values from store; fall back to global defaults if missing
        val prefs = dataStore.data.first()
        return DifficultySettings(
            useNoRepLimit = prefs[boolKey(diffKey(KEY_USE_NO_REP.name, difficulty))]
                ?: (prefs[KEY_USE_NO_REP] ?: DifficultySettings().useNoRepLimit),
            repetitions = prefs[intKey(diffKey(KEY_REPS.name, difficulty))]
                ?: (prefs[KEY_REPS] ?: DifficultySettings().repetitions),
            eccentricPercentage = prefs[floatKey(diffKey(KEY_ECCENTRIC_PERCENT.name, difficulty))]
                ?: (prefs[KEY_ECCENTRIC_PERCENT] ?: DifficultySettings().eccentricPercentage)
        )
    }

    override suspend fun saveDifficultySettings(
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        settings: DifficultySettings
    ) {
        dataStore.edit { prefs ->
            prefs[boolKey(diffKey(KEY_USE_NO_REP.name, difficulty))] = settings.useNoRepLimit
            prefs[intKey(diffKey(KEY_REPS.name, difficulty))] = settings.repetitions
            prefs[floatKey(diffKey(KEY_ECCENTRIC_PERCENT.name, difficulty))] = settings.eccentricPercentage
        }
    }

    override suspend fun resetDifficultySettings(difficulty: VitruvianDeviceManager.EchoDifficulty) {
        dataStore.edit { prefs ->
            prefs.remove(boolKey(diffKey(KEY_USE_NO_REP.name, difficulty)))
            prefs.remove(intKey(diffKey(KEY_REPS.name, difficulty)))
            prefs.remove(floatKey(diffKey(KEY_ECCENTRIC_PERCENT.name, difficulty)))
        }
    }

    override suspend fun getModeParameters(difficulty: VitruvianDeviceManager.EchoDifficulty): ModeParameters {
        val prefs = dataStore.data.first()
        val defaults = defaultModeParameters(difficulty)
        val gainKey = floatKey(modeKey("mode_gain", difficulty))
        val capKey = floatKey(modeKey("mode_cap", difficulty))
        return ModeParameters(
            gain = prefs[gainKey] ?: defaults.gain,
            capKg = prefs[capKey] ?: defaults.capKg
        )
    }

    override suspend fun saveModeParameters(
        difficulty: VitruvianDeviceManager.EchoDifficulty,
        params: ModeParameters
    ) {
        dataStore.edit { prefs ->
            prefs[floatKey(modeKey("mode_gain", difficulty))] = params.gain
            prefs[floatKey(modeKey("mode_cap", difficulty))] = params.capKg
        }
    }

    override suspend fun resetModeParameters(difficulty: VitruvianDeviceManager.EchoDifficulty) {
        dataStore.edit { prefs ->
            prefs.remove(floatKey(modeKey("mode_gain", difficulty)))
            prefs.remove(floatKey(modeKey("mode_cap", difficulty)))
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
        private val KEY_DIFFICULTY = stringPreferencesKey("difficulty")
        private val KEY_USE_NO_REP = booleanPreferencesKey("use_no_rep_limit")
        private val KEY_REPS = intPreferencesKey("reps")
        private val KEY_ECCENTRIC_PERCENT = floatPreferencesKey("eccentric_percent")
        private val KEY_LAST_DEVICE_ID = stringPreferencesKey("last_device_id")
        private val KEY_LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")

        // Helpers to create typed keys dynamically for per-difficulty settings
        private fun diffKey(base: String, difficulty: VitruvianDeviceManager.EchoDifficulty): String =
            "${base}_${difficulty.name.lowercase()}"

        private fun modeKey(base: String, difficulty: VitruvianDeviceManager.EchoDifficulty): String =
            "${base}_${difficulty.name.lowercase()}"

        private fun boolKey(name: String) = booleanPreferencesKey(name)
        private fun intKey(name: String) = intPreferencesKey(name)
        private fun floatKey(name: String) = floatPreferencesKey(name)

        private fun defaultModeParameters(d: VitruvianDeviceManager.EchoDifficulty): ModeParameters = when (d) {
            VitruvianDeviceManager.EchoDifficulty.HARD -> ModeParameters(gain = 1.0f, capKg = 50.0f)
            VitruvianDeviceManager.EchoDifficulty.HARDER -> ModeParameters(gain = 1.25f, capKg = 40.0f)
            VitruvianDeviceManager.EchoDifficulty.HARDEST -> ModeParameters(gain = 1.667f, capKg = 30.0f)
            VitruvianDeviceManager.EchoDifficulty.EPIC -> ModeParameters(gain = 3.333f, capKg = 15.0f)
        }
    }
}
