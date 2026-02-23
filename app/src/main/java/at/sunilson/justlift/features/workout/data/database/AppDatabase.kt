package at.sunilson.justlift.features.workout.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WorkoutHistoryEntity::class, ExerciseEntity::class, ExerciseSampleEntity::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutHistoryDao(): WorkoutHistoryDao

    companion object {
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercise_samples` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL DEFAULT 'WARMUP',
                        `timestampMillis` INTEGER NOT NULL,
                        `minPositionLeft` REAL NOT NULL DEFAULT 0.0,
                        `maxPositionLeft` REAL NOT NULL DEFAULT 0.0,
                        `minPositionRight` REAL NOT NULL DEFAULT 0.0,
                        `maxPositionRight` REAL NOT NULL DEFAULT 0.0,
                        `avgMinPositionLeft` REAL NOT NULL DEFAULT 0.0,
                        `avgMaxPositionLeft` REAL NOT NULL DEFAULT 0.0,
                        `avgMinPositionRight` REAL NOT NULL DEFAULT 0.0,
                        `avgMaxPositionRight` REAL NOT NULL DEFAULT 0.0,
                        `avgUpwardRepDurationMillis` REAL NOT NULL DEFAULT 0.0,
                        `avgDownwardRepDurationMillis` REAL NOT NULL DEFAULT 0.0,
                        `avgUpwardPeakForcePosition` REAL NOT NULL DEFAULT 0.0,
                        `avgDownwardPeakForcePosition` REAL NOT NULL DEFAULT 0.0,
                        `avgUpwardMaxVelocity` REAL NOT NULL DEFAULT 0.0,
                        `avgDownwardMaxVelocity` REAL NOT NULL DEFAULT 0.0,
                        `avgRestDurationMillis` REAL NOT NULL DEFAULT 0.0
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_samples_userId_exerciseName` ON `exercise_samples` (`userId`, `exerciseName`)")

                // Seed exercise_samples from existing exercises table.
                // Use a timestamp of 60 days ago so migrated data has moderate time-decay weight.
                val sixtyDaysAgoMillis = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)
                db.execSQL("""
                    INSERT INTO exercise_samples (
                        userId, exerciseName, difficulty, timestampMillis,
                        minPositionLeft, maxPositionLeft, minPositionRight, maxPositionRight,
                        avgMinPositionLeft, avgMaxPositionLeft, avgMinPositionRight, avgMaxPositionRight,
                        avgUpwardRepDurationMillis, avgDownwardRepDurationMillis,
                        avgUpwardPeakForcePosition, avgDownwardPeakForcePosition,
                        avgUpwardMaxVelocity, avgDownwardMaxVelocity,
                        avgRestDurationMillis
                    )
                    SELECT
                        userId, name, difficulty, $sixtyDaysAgoMillis,
                        minPositionLeft, maxPositionLeft, minPositionRight, maxPositionRight,
                        avgMinPositionLeft, avgMaxPositionLeft, avgMinPositionRight, avgMaxPositionRight,
                        avgUpwardRepDurationMillis, avgDownwardRepDurationMillis,
                        avgUpwardPeakForcePosition, avgDownwardPeakForcePosition,
                        avgUpwardMaxVelocity, avgDownwardMaxVelocity,
                        avgRestDurationMillis
                    FROM exercises
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_history ADD COLUMN wasAutomaticallyRecognized INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN isConfirmed INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add columns to workout_history
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgUpwardPeakForcePosition REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgDownwardPeakForcePosition REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgUpwardMaxVelocity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgDownwardMaxVelocity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgRestDurationMillis REAL NOT NULL DEFAULT 0.0")

                // Add columns to exercises
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgUpwardPeakForcePosition REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgDownwardPeakForcePosition REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgUpwardMaxVelocity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgDownwardMaxVelocity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgRestDurationMillis REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add columns to workout_history
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgUpwardRepDurationMillis REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgDownwardRepDurationMillis REAL NOT NULL DEFAULT 0.0")

                // Add columns to exercises
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgUpwardRepDurationMillis REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgDownwardRepDurationMillis REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_history ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_history ADD COLUMN exerciseName TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS `exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `name` TEXT NOT NULL, `calibratingRepsCompleted` INTEGER NOT NULL, `maxReps` INTEGER, `upwardRepetitionsCompleted` INTEGER NOT NULL, `downwardRepetitionsCompleted` INTEGER NOT NULL, `timeElapsedMillis` INTEGER NOT NULL, `averageUpwardForce` REAL NOT NULL, `averageDownwardForce` REAL NOT NULL, `maxUpwardForce` REAL NOT NULL, `maxDownwardForce` REAL NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add columns to workout_history
                db.execSQL("ALTER TABLE workout_history ADD COLUMN averageUpwardForceLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN averageUpwardForceRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN averageDownwardForceLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN averageDownwardForceRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN minPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN maxPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN minPositionRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN maxPositionRight REAL NOT NULL DEFAULT 0.0")

                // Add columns to exercises
                db.execSQL("ALTER TABLE exercises ADD COLUMN averageUpwardForceLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN averageUpwardForceRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN averageDownwardForceLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN averageDownwardForceRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN minPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN maxPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN minPositionRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN maxPositionRight REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_history ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'WARMUP'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'WARMUP'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create temporary table with unique index
                db.execSQL("CREATE TABLE IF NOT EXISTS `exercises_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `name` TEXT NOT NULL, `difficulty` TEXT NOT NULL DEFAULT 'WARMUP', `calibratingRepsCompleted` INTEGER NOT NULL, `maxReps` INTEGER, `upwardRepetitionsCompleted` INTEGER NOT NULL, `downwardRepetitionsCompleted` INTEGER NOT NULL, `timeElapsedMillis` INTEGER NOT NULL, `averageUpwardForce` REAL NOT NULL, `averageDownwardForce` REAL NOT NULL, `maxUpwardForce` REAL NOT NULL, `maxDownwardForce` REAL NOT NULL, `averageUpwardForceLeft` REAL NOT NULL DEFAULT 0.0, `averageUpwardForceRight` REAL NOT NULL DEFAULT 0.0, `averageDownwardForceLeft` REAL NOT NULL DEFAULT 0.0, `averageDownwardForceRight` REAL NOT NULL DEFAULT 0.0, `minPositionLeft` REAL NOT NULL DEFAULT 0.0, `maxPositionLeft` REAL NOT NULL DEFAULT 0.0, `minPositionRight` REAL NOT NULL DEFAULT 0.0, `maxPositionRight` REAL NOT NULL DEFAULT 0.0)")

                // Create the unique index
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercises_userId_name_difficulty` ON `exercises_new` (`userId`, `name`, `difficulty`)")

                // Copy data keeping the newest (highest ID) for each combination
                db.execSQL("""
                    INSERT INTO exercises_new (
                        id, userId, name, difficulty, calibratingRepsCompleted, maxReps, 
                        upwardRepetitionsCompleted, downwardRepetitionsCompleted, timeElapsedMillis, 
                        averageUpwardForce, averageDownwardForce, maxUpwardForce, maxDownwardForce, 
                        averageUpwardForceLeft, averageUpwardForceRight, averageDownwardForceLeft, 
                        averageDownwardForceRight, minPositionLeft, maxPositionLeft, 
                        minPositionRight, maxPositionRight
                    )
                    SELECT 
                        id, userId, name, difficulty, calibratingRepsCompleted, maxReps, 
                        upwardRepetitionsCompleted, downwardRepetitionsCompleted, timeElapsedMillis, 
                        averageUpwardForce, averageDownwardForce, maxUpwardForce, maxDownwardForce, 
                        averageUpwardForceLeft, averageUpwardForceRight, averageDownwardForceLeft, 
                        averageDownwardForceRight, minPositionLeft, maxPositionLeft, 
                        minPositionRight, maxPositionRight
                    FROM exercises 
                    WHERE id IN (SELECT MAX(id) FROM exercises GROUP BY userId, name, difficulty)
                """.trimIndent())

                // Drop old table and rename new one
                db.execSQL("DROP TABLE exercises")
                db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add columns to workout_history
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgMinPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgMaxPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgMinPositionRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE workout_history ADD COLUMN avgMaxPositionRight REAL NOT NULL DEFAULT 0.0")

                // Add columns to exercises
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgMinPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgMaxPositionLeft REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgMinPositionRight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN avgMaxPositionRight REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}
