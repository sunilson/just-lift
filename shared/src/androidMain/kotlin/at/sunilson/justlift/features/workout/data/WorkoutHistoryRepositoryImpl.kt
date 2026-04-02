package at.sunilson.justlift.features.workout.data

import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryDao
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity
import org.koin.core.annotation.Single

@Single(binds = [WorkoutHistoryRepository::class])
class WorkoutHistoryRepositoryImpl(
    private val dao: WorkoutHistoryDao
) : WorkoutHistoryRepository {
    override suspend fun insert(workout: WorkoutHistoryEntity): Long = dao.insert(workout)
    override suspend fun updateExerciseName(id: Long, exerciseName: String?) = dao.updateExerciseName(id, exerciseName)
    override suspend fun setConfirmed(id: Long) = dao.setConfirmed(id)
    override suspend fun getLatest(userId: Int): WorkoutHistoryEntity? = dao.getLatest(userId)
    override suspend fun getAllHistory(userId: Int): List<WorkoutHistoryEntity> = dao.getAllHistory(userId)
    override suspend fun getAllExerciseNames(): List<String> = dao.getAllExerciseNames()
    override suspend fun renameExercise(oldName: String, newName: String) = dao.renameExercise(oldName, newName)
    override suspend fun deleteExercise(name: String) = dao.deleteExercise(name)
    override suspend fun renameExerciseInHistory(oldName: String, newName: String?) = dao.renameExerciseInHistory(oldName, newName)
    override suspend fun deleteAll() = dao.deleteAll()
}
