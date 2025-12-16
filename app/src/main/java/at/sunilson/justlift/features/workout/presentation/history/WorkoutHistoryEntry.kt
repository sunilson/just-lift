package at.sunilson.justlift.features.workout.presentation.history

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager

data class WorkoutHistoryEntry(
    val workoutState: VitruvianDeviceManager.WorkoutState,
    val timestampMillis: Long
)
