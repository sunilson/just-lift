package at.sunilson.justlift.features.workout.data

import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.database.ExerciseSampleEntity

fun VitruvianDeviceManager.WorkoutState.normalized(): VitruvianDeviceManager.WorkoutState {
    val leftRange = maxPositionLeft - minPositionLeft
    val rightRange = maxPositionRight - minPositionRight

    // If only right cable is active, swap
    if (rightRange > 0.05 && leftRange <= 0.05) {
        return copy(
            averageUpwardForceLeft = averageUpwardForceRight,
            averageUpwardForceRight = averageUpwardForceLeft,
            averageDownwardForceLeft = averageDownwardForceRight,
            averageDownwardForceRight = averageDownwardForceLeft,
            minPositionLeft = minPositionRight,
            maxPositionLeft = maxPositionRight,
            minPositionRight = minPositionLeft,
            maxPositionRight = maxPositionLeft,
            avgMinPositionLeft = avgMinPositionRight,
            avgMaxPositionLeft = avgMaxPositionRight,
            avgMinPositionRight = avgMinPositionLeft,
            avgMaxPositionRight = avgMaxPositionLeft
        )
    }
    return this
}

fun ExerciseEntity.normalized(): ExerciseEntity {
    val leftRange = maxPositionLeft - minPositionLeft
    val rightRange = maxPositionRight - minPositionRight

    // If only right cable is active, swap
    if (rightRange > 0.05 && leftRange <= 0.05) {
        return copy(
            averageUpwardForceLeft = averageUpwardForceRight,
            averageUpwardForceRight = averageUpwardForceLeft,
            averageDownwardForceLeft = averageDownwardForceRight,
            averageDownwardForceRight = averageDownwardForceLeft,
            minPositionLeft = minPositionRight,
            maxPositionLeft = maxPositionRight,
            minPositionRight = minPositionLeft,
            maxPositionRight = maxPositionLeft,
            avgMinPositionLeft = avgMinPositionRight,
            avgMaxPositionLeft = avgMaxPositionRight,
            avgMinPositionRight = avgMinPositionLeft,
            avgMaxPositionRight = avgMaxPositionLeft
        )
    }
    return this
}

fun ExerciseSampleEntity.normalized(): ExerciseSampleEntity {
    val leftRange = maxPositionLeft - minPositionLeft
    val rightRange = maxPositionRight - minPositionRight

    // If only right cable is active, swap position fields
    if (rightRange > 0.05 && leftRange <= 0.05) {
        return copy(
            minPositionLeft = minPositionRight,
            maxPositionLeft = maxPositionRight,
            minPositionRight = minPositionLeft,
            maxPositionRight = maxPositionLeft,
            avgMinPositionLeft = avgMinPositionRight,
            avgMaxPositionLeft = avgMaxPositionRight,
            avgMinPositionRight = avgMinPositionLeft,
            avgMaxPositionRight = avgMaxPositionLeft
        )
    }
    return this
}
