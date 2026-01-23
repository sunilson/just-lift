package at.sunilson.justlift.features.workout.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class WorkoutHistoryWithStats(
    @Embedded val entity: WorkoutHistoryEntity,
    val averageVolumeOfOthers: Double?
)

@Dao
interface WorkoutHistoryDao {
    @Insert
    suspend fun insert(workout: WorkoutHistoryEntity): Long

    @Query("UPDATE workout_history SET exerciseName = :exerciseName, wasAutomaticallyRecognized = 0 WHERE id = :id")
    suspend fun updateExerciseName(id: Long, exerciseName: String?)

    @Query("UPDATE workout_history SET isConfirmed = 1 WHERE id = :id")
    suspend fun setConfirmed(id: Long)

    @Query("""
        SELECT *, 
               (SELECT AVG((upwardRepetitionsCompleted * averageUpwardForce) + (downwardRepetitionsCompleted * averageDownwardForce)) 
                FROM workout_history as wh2 
                WHERE wh2.exerciseName = workout_history.exerciseName 
                  AND wh2.difficulty = workout_history.difficulty
                  AND wh2.userId = workout_history.userId 
                  AND wh2.id != workout_history.id
                  AND wh2.exerciseName IS NOT NULL) as averageVolumeOfOthers
        FROM workout_history 
        WHERE userId = :userId 
        ORDER BY timestampMillis DESC
    """)
    fun getAllPaged(userId: Int): PagingSource<Int, WorkoutHistoryWithStats>

    @Query("SELECT * FROM workout_history WHERE userId = :userId ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatest(userId: Int): WorkoutHistoryEntity?

    @Query("SELECT * FROM workout_history WHERE userId = :userId ORDER BY timestampMillis ASC")
    suspend fun getAllHistory(userId: Int): List<WorkoutHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE userId = :userId")
    suspend fun getAllExercises(userId: Int): List<ExerciseEntity>

    @Query("SELECT DISTINCT name FROM exercises")
    suspend fun getAllExerciseNames(): List<String>

    @Query("SELECT * FROM exercises WHERE userId = :userId AND name = :name AND difficulty = :difficulty LIMIT 1")
    suspend fun getExercise(userId: Int, name: String, difficulty: String): ExerciseEntity?

    @Query("UPDATE exercises SET name = :newName WHERE name = :oldName")
    suspend fun renameExercise(oldName: String, newName: String)

    @Query("DELETE FROM exercises WHERE name = :name")
    suspend fun deleteExercise(name: String)

    @Query("UPDATE workout_history SET exerciseName = :newName WHERE exerciseName = :oldName")
    suspend fun renameExerciseInHistory(oldName: String, newName: String?)

    @Query("DELETE FROM workout_history")
    suspend fun deleteAll()
}
