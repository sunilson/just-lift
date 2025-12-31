package at.sunilson.justlift.features.workout.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutHistoryDao {
    @Insert
    suspend fun insert(workout: WorkoutHistoryEntity)

    @Query("UPDATE workout_history SET exerciseName = :exerciseName WHERE id = :id")
    suspend fun updateExerciseName(id: Long, exerciseName: String?)

    @Query("SELECT * FROM workout_history WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun getAllPaged(userId: Int): PagingSource<Int, WorkoutHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE userId = :userId")
    suspend fun getAllExercises(userId: Int): List<ExerciseEntity>

    @Query("DELETE FROM workout_history")
    suspend fun deleteAll()
}
