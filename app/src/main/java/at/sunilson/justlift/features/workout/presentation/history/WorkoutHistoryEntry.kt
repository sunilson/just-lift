package at.sunilson.justlift.features.workout.presentation.history

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity
import kotlin.time.Duration.Companion.milliseconds

data class WorkoutHistoryEntry(
    val workoutState: VitruvianDeviceManager.WorkoutState,
    val timestampMillis: Long
)

sealed class WorkoutHistoryUiModel {
    data class Entry(val entry: WorkoutHistoryEntry) : WorkoutHistoryUiModel()
    data class Header(val date: String) : WorkoutHistoryUiModel()
}

fun WorkoutHistoryEntity.toDomain() = WorkoutHistoryEntry(
    timestampMillis = timestampMillis,
    workoutState = VitruvianDeviceManager.WorkoutState(
        calibratingRepsCompleted = calibratingRepsCompleted,
        maxReps = maxReps,
        upwardRepetitionsCompleted = upwardRepetitionsCompleted,
        downwardRepetitionsCompleted = downwardRepetitionsCompleted,
        timeElapsed = timeElapsedMillis.milliseconds,
        averageUpwardForce = averageUpwardForce,
        averageDownwardForce = averageDownwardForce,
        maxUpwardForce = maxUpwardForce,
        maxDownwardForce = maxDownwardForce
    )
)

fun WorkoutHistoryEntry.toEntity() = WorkoutHistoryEntity(
    timestampMillis = timestampMillis,
    calibratingRepsCompleted = workoutState.calibratingRepsCompleted,
    maxReps = workoutState.maxReps,
    upwardRepetitionsCompleted = workoutState.upwardRepetitionsCompleted,
    downwardRepetitionsCompleted = workoutState.downwardRepetitionsCompleted,
    timeElapsedMillis = workoutState.timeElapsed.inWholeMilliseconds,
    averageUpwardForce = workoutState.averageUpwardForce,
    averageDownwardForce = workoutState.averageDownwardForce,
    maxUpwardForce = workoutState.maxUpwardForce,
    maxDownwardForce = workoutState.maxDownwardForce
)
