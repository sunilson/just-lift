package at.sunilson.justlift.features.workout.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_history")
data class WorkoutHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int = 1,
    val timestampMillis: Long,
    val calibratingRepsCompleted: Int,
    val maxReps: Int?,
    val upwardRepetitionsCompleted: Int,
    val downwardRepetitionsCompleted: Int,
    val timeElapsedMillis: Long,
    val averageUpwardForce: Double,
    val averageDownwardForce: Double,
    val maxUpwardForce: Double,
    val maxDownwardForce: Double,
    val averageUpwardForceLeft: Double = 0.0,
    val averageUpwardForceRight: Double = 0.0,
    val averageDownwardForceLeft: Double = 0.0,
    val averageDownwardForceRight: Double = 0.0,
    val minPositionLeft: Double = 0.0,
    val maxPositionLeft: Double = 0.0,
    val minPositionRight: Double = 0.0,
    val maxPositionRight: Double = 0.0,
    val avgMinPositionLeft: Double = 0.0,
    val avgMaxPositionLeft: Double = 0.0,
    val avgMinPositionRight: Double = 0.0,
    val avgMaxPositionRight: Double = 0.0,
    val exerciseName: String? = null,
    val difficulty: String = "WARMUP"
)

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["userId", "name", "difficulty"], unique = true)]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val name: String,
    val difficulty: String = "WARMUP",
    val calibratingRepsCompleted: Int,
    val maxReps: Int?,
    val upwardRepetitionsCompleted: Int,
    val downwardRepetitionsCompleted: Int,
    val timeElapsedMillis: Long,
    val averageUpwardForce: Double,
    val averageDownwardForce: Double,
    val maxUpwardForce: Double,
    val maxDownwardForce: Double,
    val averageUpwardForceLeft: Double = 0.0,
    val averageUpwardForceRight: Double = 0.0,
    val averageDownwardForceLeft: Double = 0.0,
    val averageDownwardForceRight: Double = 0.0,
    val minPositionLeft: Double = 0.0,
    val maxPositionLeft: Double = 0.0,
    val minPositionRight: Double = 0.0,
    val maxPositionRight: Double = 0.0,
    val avgMinPositionLeft: Double = 0.0,
    val avgMaxPositionLeft: Double = 0.0,
    val avgMinPositionRight: Double = 0.0,
    val avgMaxPositionRight: Double = 0.0
)
