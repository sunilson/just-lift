package at.sunilson.justlift.features.workout.data.database

import androidx.room.Embedded

data class WorkoutHistoryWithStats(
    @Embedded val entity: WorkoutHistoryEntity,
    val averageVolumeOfOthers: Double?
)
