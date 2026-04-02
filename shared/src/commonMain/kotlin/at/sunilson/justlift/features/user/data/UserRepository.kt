package at.sunilson.justlift.features.user.data

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val currentUserId: Flow<Int>
    val twoUserMode: Flow<Boolean>
    suspend fun switchToUser(userId: Int)
    suspend fun setTwoUserMode(enabled: Boolean)
}
