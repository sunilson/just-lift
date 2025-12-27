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

@Single
class UserRepository(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")
    private val KEY_CURRENT_USER_ID = intPreferencesKey("current_user_id")

    val currentUserId: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENT_USER_ID] ?: 1
    }

    suspend fun switchToUser(userId: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENT_USER_ID] = userId
        }
    }
}
