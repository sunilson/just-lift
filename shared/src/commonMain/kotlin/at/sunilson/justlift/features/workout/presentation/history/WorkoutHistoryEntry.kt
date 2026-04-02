package at.sunilson.justlift.features.workout.presentation.history

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryWithStats
import kotlin.time.Duration.Companion.milliseconds

data class WorkoutHistoryEntry(
    val id: Long = 0,
    val workoutState: VitruvianDeviceManager.WorkoutState,
    val timestampMillis: Long,
    val exerciseName: String? = null,
    val score: Double? = null
)

data class ExerciseTrend(
    val exerciseName: String,
    val avgUpwardTrend: Double,
    val avgDownwardTrend: Double,
    val recentUpwardTrend: Double = 0.0,
    val recentDownwardTrend: Double = 0.0
)

enum class TrendTimeframe(val label: String) {
    ONE_WEEK("1 week"),
    TWO_WEEKS("2 weeks"),
    ONE_MONTH("1 month"),
    THREE_MONTHS("3 months"),
    ONE_YEAR("1 year")
}

sealed class WorkoutHistoryUiModel {
    data class Entry(val entry: WorkoutHistoryEntry) : WorkoutHistoryUiModel()
    data class Header(val date: String) : WorkoutHistoryUiModel()
}

fun WorkoutHistoryWithStats.toDomain(): WorkoutHistoryEntry {
    val domain = entity.toDomain()
    val averageVolumeOfOthers = averageVolumeOfOthers ?: return domain
    if (averageVolumeOfOthers <= 0) return domain

    val currentVolume = (entity.upwardRepetitionsCompleted * entity.averageUpwardForce) + (entity.downwardRepetitionsCompleted * entity.averageDownwardForce)
    val score = ((currentVolume - averageVolumeOfOthers) / averageVolumeOfOthers) * 100.0
    return domain.copy(score = score)
}

fun WorkoutHistoryEntity.toDomain() = WorkoutHistoryEntry(
    id = id,
    timestampMillis = timestampMillis,
    exerciseName = exerciseName,
    workoutState = VitruvianDeviceManager.WorkoutState(
        calibratingRepsCompleted = calibratingRepsCompleted,
        maxReps = maxReps,
        upwardRepetitionsCompleted = upwardRepetitionsCompleted,
        downwardRepetitionsCompleted = downwardRepetitionsCompleted,
        timeElapsed = timeElapsedMillis.milliseconds,
        averageUpwardForce = averageUpwardForce,
        averageDownwardForce = averageDownwardForce,
        maxUpwardForce = maxUpwardForce,
        maxDownwardForce = maxDownwardForce,
        averageUpwardForceLeft = averageUpwardForceLeft,
        averageUpwardForceRight = averageUpwardForceRight,
        averageDownwardForceLeft = averageDownwardForceLeft,
        averageDownwardForceRight = averageDownwardForceRight,
        minPositionLeft = minPositionLeft,
        maxPositionLeft = maxPositionLeft,
        minPositionRight = minPositionRight,
        maxPositionRight = maxPositionRight,
        avgMinPositionLeft = avgMinPositionLeft,
        avgMaxPositionLeft = avgMaxPositionLeft,
        avgMinPositionRight = avgMinPositionRight,
        avgMaxPositionRight = avgMaxPositionRight,
        avgUpwardRepDurationMillis = avgUpwardRepDurationMillis,
        avgDownwardRepDurationMillis = avgDownwardRepDurationMillis,
        avgUpwardPeakForcePosition = avgUpwardPeakForcePosition,
        avgDownwardPeakForcePosition = avgDownwardPeakForcePosition,
        avgUpwardMaxVelocity = avgUpwardMaxVelocity,
        avgDownwardMaxVelocity = avgDownwardMaxVelocity,
        avgRestDurationMillis = avgRestDurationMillis,
        difficulty = VitruvianDeviceManager.EchoDifficulty.valueOf(difficulty)
    )
)

/**
 * Calculates the estimated 1-Rep Max for the upward phase using the Epley formula.
 * Formula: Weight * (1 + Reps / 30)
 */
fun WorkoutHistoryEntity.estimatedUpwardOneRepMax(): Double {
    return averageUpwardForce * (1.0 + upwardRepetitionsCompleted / 30.0)
}

/**
 * Calculates the estimated 1-Rep Max for the downward phase using the Epley formula.
 * Formula: Weight * (1 + Reps / 30)
 */
fun WorkoutHistoryEntity.estimatedDownwardOneRepMax(): Double {
    return averageDownwardForce * (1.0 + downwardRepetitionsCompleted / 30.0)
}

fun WorkoutHistoryEntry.toEntity(userId: Int) = WorkoutHistoryEntity(
    id = id,
    userId = userId,
    timestampMillis = timestampMillis,
    calibratingRepsCompleted = workoutState.calibratingRepsCompleted,
    maxReps = workoutState.maxReps,
    upwardRepetitionsCompleted = workoutState.upwardRepetitionsCompleted,
    downwardRepetitionsCompleted = workoutState.downwardRepetitionsCompleted,
    timeElapsedMillis = workoutState.timeElapsed.inWholeMilliseconds,
    averageUpwardForce = workoutState.averageUpwardForce,
    averageDownwardForce = workoutState.averageDownwardForce,
    maxUpwardForce = workoutState.maxUpwardForce,
    maxDownwardForce = workoutState.maxDownwardForce,
    averageUpwardForceLeft = workoutState.averageUpwardForceLeft,
    averageUpwardForceRight = workoutState.averageUpwardForceRight,
    averageDownwardForceLeft = workoutState.averageDownwardForceLeft,
    averageDownwardForceRight = workoutState.averageDownwardForceRight,
    minPositionLeft = workoutState.minPositionLeft,
    maxPositionLeft = workoutState.maxPositionLeft,
    minPositionRight = workoutState.minPositionRight,
    maxPositionRight = workoutState.maxPositionRight,
    avgMinPositionLeft = workoutState.avgMinPositionLeft,
    avgMaxPositionLeft = workoutState.avgMaxPositionLeft,
    avgMinPositionRight = workoutState.avgMinPositionRight,
    avgMaxPositionRight = workoutState.avgMaxPositionRight,
    avgUpwardRepDurationMillis = workoutState.avgUpwardRepDurationMillis,
    avgDownwardRepDurationMillis = workoutState.avgDownwardRepDurationMillis,
    avgUpwardPeakForcePosition = workoutState.avgUpwardPeakForcePosition,
    avgDownwardPeakForcePosition = workoutState.avgDownwardPeakForcePosition,
    avgUpwardMaxVelocity = workoutState.avgUpwardMaxVelocity,
    avgDownwardMaxVelocity = workoutState.avgDownwardMaxVelocity,
    avgRestDurationMillis = workoutState.avgRestDurationMillis,
    exerciseName = exerciseName,
    difficulty = workoutState.difficulty.name,
    wasAutomaticallyRecognized = false,
    isConfirmed = false
)
