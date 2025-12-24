package at.sunilson.justlift.features.workout.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkoutHistoryDao {
    @Insert
    suspend fun insert(workout: WorkoutHistoryEntity)

    @Query("SELECT * FROM workout_history ORDER BY timestampMillis DESC")
    fun getAllPaged(): PagingSource<Int, WorkoutHistoryEntity>

    @Query("DELETE FROM workout_history")
    suspend fun deleteAll()
}
