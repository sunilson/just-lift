package at.sunilson.justlift.features.workout.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_history")
data class WorkoutHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val calibratingRepsCompleted: Int,
    val maxReps: Int?,
    val upwardRepetitionsCompleted: Int,
    val downwardRepetitionsCompleted: Int,
    val timeElapsedMillis: Long,
    val averageUpwardForce: Double,
    val averageDownwardForce: Double,
    val maxUpwardForce: Double,
    val maxDownwardForce: Double
)
