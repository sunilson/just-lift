package at.sunilson.justlift.features.workout.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WorkoutHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
}
