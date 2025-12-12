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
    }
}
