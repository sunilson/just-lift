package at.sunilson.justlift.features.user.data

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val currentUserId: Flow<Int>
    val twoUserMode: Flow<Boolean>
    val setsPerUser: Flow<Int>
    suspend fun switchToUser(userId: Int)
    suspend fun setTwoUserMode(enabled: Boolean)
    suspend fun setSetsPerUser(value: Int)
}
