package at.sunilson.justlift.features.workout.data.database

import android.content.Context
import androidx.room.Room
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class DatabaseModule {
    @Single
    fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "just-lift-db"
        ).build()
    }

    @Single
    fun provideWorkoutHistoryDao(database: AppDatabase): WorkoutHistoryDao {
        return database.workoutHistoryDao()
    }
}
