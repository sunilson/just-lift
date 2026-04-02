package at.sunilson.justlift.features.workout.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@Database(
    entities = [WorkoutHistoryEntity::class, ExerciseEntity::class, ExerciseSampleEntity::class],
    version = 11,
    exportSchema = false
)
@ConstructedBy(IosAppDatabaseConstructor::class)
abstract class IosAppDatabase : RoomDatabase() {
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
}

// Room KSP will generate the actual implementation
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object IosAppDatabaseConstructor : RoomDatabaseConstructor<IosAppDatabase>

fun createIosDatabase(): IosAppDatabase {
    val docDir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).first() as String
    val dbPath = "$docDir/just-lift-db"
    return Room.databaseBuilder<IosAppDatabase>(
        name = dbPath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
