package at.sunilson.justlift.features.workout.data

import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity

/**
 * Cross-platform interface for workout history persistence.
 * Android implementation delegates to Room DAO; iOS will provide its own implementation.
 */
interface WorkoutHistoryRepository {
    suspend fun insert(workout: WorkoutHistoryEntity): Long
    suspend fun updateExerciseName(id: Long, exerciseName: String?)
    suspend fun setConfirmed(id: Long)
    suspend fun getLatest(userId: Int): WorkoutHistoryEntity?
    suspend fun getAllHistory(userId: Int): List<WorkoutHistoryEntity>
    suspend fun getAllExerciseNames(): List<String>
    suspend fun renameExercise(oldName: String, newName: String)
    suspend fun deleteExercise(name: String)
    suspend fun renameExerciseInHistory(oldName: String, newName: String?)
    suspend fun deleteAll()
}
