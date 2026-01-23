package at.sunilson.justlift.features.workout.presentation.history

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryWithStats
import at.sunilson.justlift.features.workout.data.normalized
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

data class WorkoutHistoryEntry(
    val id: Long = 0,
    val workoutState: VitruvianDeviceManager.WorkoutState,
    val timestampMillis: Long,
    val exerciseName: String? = null,
    val score: Double? = null,
    val wasAutomaticallyRecognized: Boolean = false,
    val isConfirmed: Boolean = false
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
    ),
    wasAutomaticallyRecognized = wasAutomaticallyRecognized,
    isConfirmed = isConfirmed
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
    wasAutomaticallyRecognized = wasAutomaticallyRecognized,
    isConfirmed = isConfirmed
)

fun VitruvianDeviceManager.WorkoutState.toExerciseEntity(userId: Int, name: String): ExerciseEntity {
    val normalized = this.normalized()
    return ExerciseEntity(
        userId = userId,
        name = name,
        difficulty = difficulty.name,
        calibratingRepsCompleted = normalized.calibratingRepsCompleted,
        maxReps = normalized.maxReps,
        upwardRepetitionsCompleted = normalized.upwardRepetitionsCompleted,
        downwardRepetitionsCompleted = normalized.downwardRepetitionsCompleted,
        timeElapsedMillis = normalized.timeElapsed.inWholeMilliseconds,
        averageUpwardForce = normalized.averageUpwardForce,
        averageDownwardForce = normalized.averageDownwardForce,
        maxUpwardForce = normalized.maxUpwardForce,
        maxDownwardForce = normalized.maxDownwardForce,
        averageUpwardForceLeft = normalized.averageUpwardForceLeft,
        averageUpwardForceRight = normalized.averageUpwardForceRight,
        averageDownwardForceLeft = normalized.averageDownwardForceLeft,
        averageDownwardForceRight = normalized.averageDownwardForceRight,
        minPositionLeft = normalized.minPositionLeft,
        maxPositionLeft = normalized.maxPositionLeft,
        minPositionRight = normalized.minPositionRight,
        maxPositionRight = normalized.maxPositionRight,
        avgMinPositionLeft = normalized.avgMinPositionLeft,
        avgMaxPositionLeft = normalized.avgMaxPositionLeft,
        avgMinPositionRight = normalized.avgMinPositionRight,
        avgMaxPositionRight = normalized.avgMaxPositionRight,
        avgUpwardRepDurationMillis = normalized.avgUpwardRepDurationMillis,
        avgDownwardRepDurationMillis = normalized.avgDownwardRepDurationMillis,
        avgUpwardPeakForcePosition = normalized.avgUpwardPeakForcePosition,
        avgDownwardPeakForcePosition = normalized.avgDownwardPeakForcePosition,
        avgUpwardMaxVelocity = normalized.avgUpwardMaxVelocity,
        avgDownwardMaxVelocity = normalized.avgDownwardMaxVelocity,
        avgRestDurationMillis = normalized.avgRestDurationMillis
    )
}

fun ExerciseEntity.averageWith(workoutState: VitruvianDeviceManager.WorkoutState): ExerciseEntity {
    val normalizedEntity = this.normalized()
    val normalizedState = workoutState.normalized()

    return normalizedEntity.copy(
        calibratingRepsCompleted = ((normalizedEntity.calibratingRepsCompleted + normalizedState.calibratingRepsCompleted) / 2.0).roundToInt(),
        maxReps = if (normalizedEntity.maxReps != null && normalizedState.maxReps != null) {
            ((normalizedEntity.maxReps + normalizedState.maxReps) / 2.0).roundToInt()
        } else {
            normalizedEntity.maxReps ?: normalizedState.maxReps
        },
        upwardRepetitionsCompleted = ((normalizedEntity.upwardRepetitionsCompleted + normalizedState.upwardRepetitionsCompleted) / 2.0).roundToInt(),
        downwardRepetitionsCompleted = ((normalizedEntity.downwardRepetitionsCompleted + normalizedState.downwardRepetitionsCompleted) / 2.0).roundToInt(),
        timeElapsedMillis = ((normalizedEntity.timeElapsedMillis + normalizedState.timeElapsed.inWholeMilliseconds) / 2.0).toLong(),
        averageUpwardForce = (normalizedEntity.averageUpwardForce + normalizedState.averageUpwardForce) / 2.0,
        averageDownwardForce = (normalizedEntity.averageDownwardForce + normalizedState.averageDownwardForce) / 2.0,
        maxUpwardForce = (normalizedEntity.maxUpwardForce + normalizedState.maxUpwardForce) / 2.0,
        maxDownwardForce = (normalizedEntity.maxDownwardForce + normalizedState.maxDownwardForce) / 2.0,
        averageUpwardForceLeft = (normalizedEntity.averageUpwardForceLeft + normalizedState.averageUpwardForceLeft) / 2.0,
        averageUpwardForceRight = (normalizedEntity.averageUpwardForceRight + normalizedState.averageUpwardForceRight) / 2.0,
        averageDownwardForceLeft = (normalizedEntity.averageDownwardForceLeft + normalizedState.averageDownwardForceLeft) / 2.0,
        averageDownwardForceRight = (normalizedEntity.averageDownwardForceRight + normalizedState.averageDownwardForceRight) / 2.0,
        minPositionLeft = (normalizedEntity.minPositionLeft + normalizedState.minPositionLeft) / 2.0,
        maxPositionLeft = (normalizedEntity.maxPositionLeft + normalizedState.maxPositionLeft) / 2.0,
        minPositionRight = (normalizedEntity.minPositionRight + normalizedState.minPositionRight) / 2.0,
        maxPositionRight = (normalizedEntity.maxPositionRight + normalizedState.maxPositionRight) / 2.0,
        avgMinPositionLeft = (normalizedEntity.avgMinPositionLeft + normalizedState.avgMinPositionLeft) / 2.0,
        avgMaxPositionLeft = (normalizedEntity.avgMaxPositionLeft + normalizedState.avgMaxPositionLeft) / 2.0,
        avgMinPositionRight = (normalizedEntity.avgMinPositionRight + normalizedState.avgMinPositionRight) / 2.0,
        avgMaxPositionRight = (normalizedEntity.avgMaxPositionRight + normalizedState.avgMaxPositionRight) / 2.0,
        avgUpwardRepDurationMillis = (normalizedEntity.avgUpwardRepDurationMillis + normalizedState.avgUpwardRepDurationMillis) / 2.0,
        avgDownwardRepDurationMillis = (normalizedEntity.avgDownwardRepDurationMillis + normalizedState.avgDownwardRepDurationMillis) / 2.0,
        avgUpwardPeakForcePosition = (normalizedEntity.avgUpwardPeakForcePosition + normalizedState.avgUpwardPeakForcePosition) / 2.0,
        avgDownwardPeakForcePosition = (normalizedEntity.avgDownwardPeakForcePosition + normalizedState.avgDownwardPeakForcePosition) / 2.0,
        avgUpwardMaxVelocity = (normalizedEntity.avgUpwardMaxVelocity + normalizedState.avgUpwardMaxVelocity) / 2.0,
        avgDownwardMaxVelocity = (normalizedEntity.avgDownwardMaxVelocity + normalizedState.avgDownwardMaxVelocity) / 2.0,
        avgRestDurationMillis = (normalizedEntity.avgRestDurationMillis + normalizedState.avgRestDurationMillis) / 2.0
    )
}

