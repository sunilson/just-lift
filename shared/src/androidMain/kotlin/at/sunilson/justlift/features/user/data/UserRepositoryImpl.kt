package at.sunilson.justlift.features.user.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [UserRepository::class])
class UserRepositoryImpl(private val context: Context) : UserRepository {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")
    private val KEY_CURRENT_USER_ID = intPreferencesKey("current_user_id")
    private val KEY_TWO_USER_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("two_user_mode")

    override val currentUserId: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENT_USER_ID] ?: 1
    }

    override val twoUserMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_TWO_USER_MODE] ?: false
    }

    override suspend fun switchToUser(userId: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENT_USER_ID] = userId
        }
    }

    override suspend fun setTwoUserMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TWO_USER_MODE] = enabled
        }
    }
}
