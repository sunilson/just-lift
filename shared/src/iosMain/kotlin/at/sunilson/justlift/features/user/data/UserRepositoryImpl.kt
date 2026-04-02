package at.sunilson.justlift.features.user.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class UserRepositoryImpl : UserRepository {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val docDir = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, true
            ).first() as String
            "$docDir/user_settings.preferences_pb".toPath()
        }
    )
    private val KEY_CURRENT_USER_ID = intPreferencesKey("current_user_id")
    private val KEY_TWO_USER_MODE = booleanPreferencesKey("two_user_mode")

    override val currentUserId: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENT_USER_ID] ?: 1
    }

    override val twoUserMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_TWO_USER_MODE] ?: false
    }

    override suspend fun switchToUser(userId: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENT_USER_ID] = userId
        }
    }

    override suspend fun setTwoUserMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TWO_USER_MODE] = enabled
        }
    }
}
