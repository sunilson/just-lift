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
    suspend fun insert(workout: WorkoutHistoryEntity)

    @Query("UPDATE workout_history SET exerciseName = :exerciseName WHERE id = :id")
    suspend fun updateExerciseName(id: Long, exerciseName: String?)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE userId = :userId")
    suspend fun getAllExercises(userId: Int): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE userId = :userId AND name = :name AND difficulty = :difficulty LIMIT 1")
    suspend fun getExercise(userId: Int, name: String, difficulty: String): ExerciseEntity?

    @Query("DELETE FROM workout_history")
    suspend fun deleteAll()
}
