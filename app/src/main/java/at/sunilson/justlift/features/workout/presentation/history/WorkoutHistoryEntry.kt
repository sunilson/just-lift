package at.sunilson.justlift.features.workout.presentation.history

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.database.ExerciseSampleEntity
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

fun VitruvianDeviceManager.WorkoutState.toExerciseSample(
    userId: Int,
    exerciseName: String,
    timestampMillis: Long = System.currentTimeMillis()
): ExerciseSampleEntity {
    val normalized = this.normalized()
    return ExerciseSampleEntity(
        userId = userId,
        exerciseName = exerciseName,
        difficulty = normalized.difficulty.name,
        timestampMillis = timestampMillis,
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

/**
 * Averages this exercise fingerprint with a new workout state using weighted averaging.
 *
 * Key design decisions:
 * 1. Position-based metrics (ROM, peak positions) use slower averaging (0.3 weight) to maintain
 *    stable fingerprints that accurately identify the exercise movement pattern.
 * 2. Force and rep metrics are NOT averaged - they vary based on training intensity (volume vs weight)
 *    and progressive overload. Averaging them would create fingerprints that don't match any
 *    real workout pattern.
 * 3. Timing metrics (rep duration, rest) use moderate averaging (0.3 weight) as they're somewhat
 *    characteristic of the exercise but can vary with fatigue.
 */
fun ExerciseEntity.averageWith(workoutState: VitruvianDeviceManager.WorkoutState): ExerciseEntity {
    val normalizedEntity = this.normalized()
    val normalizedState = workoutState.normalized()

    // Weight for exponential moving average of position metrics
    // Lower weight = slower adaptation = more stable fingerprint
    val positionWeight = 0.3
    val timingWeight = 0.3

    fun weightedAvg(old: Double, new: Double, weight: Double): Double =
        old * (1 - weight) + new * weight

    return normalizedEntity.copy(
        // Keep calibrating reps and maxReps as-is (they're session settings, not fingerprint data)
        calibratingRepsCompleted = normalizedEntity.calibratingRepsCompleted,
        maxReps = normalizedEntity.maxReps,
        // Don't average force/rep metrics - they vary with training intensity
        // Keep the original fingerprint values to avoid drift from volume/weight variations
        upwardRepetitionsCompleted = normalizedEntity.upwardRepetitionsCompleted,
        downwardRepetitionsCompleted = normalizedEntity.downwardRepetitionsCompleted,
        timeElapsedMillis = normalizedEntity.timeElapsedMillis,
        averageUpwardForce = normalizedEntity.averageUpwardForce,
        averageDownwardForce = normalizedEntity.averageDownwardForce,
        maxUpwardForce = normalizedEntity.maxUpwardForce,
        maxDownwardForce = normalizedEntity.maxDownwardForce,
        averageUpwardForceLeft = normalizedEntity.averageUpwardForceLeft,
        averageUpwardForceRight = normalizedEntity.averageUpwardForceRight,
        averageDownwardForceLeft = normalizedEntity.averageDownwardForceLeft,
        averageDownwardForceRight = normalizedEntity.averageDownwardForceRight,
        // Position metrics - use weighted averaging for stable movement pattern recognition
        minPositionLeft = weightedAvg(normalizedEntity.minPositionLeft, normalizedState.minPositionLeft, positionWeight),
        maxPositionLeft = weightedAvg(normalizedEntity.maxPositionLeft, normalizedState.maxPositionLeft, positionWeight),
        minPositionRight = weightedAvg(normalizedEntity.minPositionRight, normalizedState.minPositionRight, positionWeight),
        maxPositionRight = weightedAvg(normalizedEntity.maxPositionRight, normalizedState.maxPositionRight, positionWeight),
        avgMinPositionLeft = weightedAvg(normalizedEntity.avgMinPositionLeft, normalizedState.avgMinPositionLeft, positionWeight),
        avgMaxPositionLeft = weightedAvg(normalizedEntity.avgMaxPositionLeft, normalizedState.avgMaxPositionLeft, positionWeight),
        avgMinPositionRight = weightedAvg(normalizedEntity.avgMinPositionRight, normalizedState.avgMinPositionRight, positionWeight),
        avgMaxPositionRight = weightedAvg(normalizedEntity.avgMaxPositionRight, normalizedState.avgMaxPositionRight, positionWeight),
        // Timing metrics - moderately stable, can vary with fatigue
        avgUpwardRepDurationMillis = weightedAvg(normalizedEntity.avgUpwardRepDurationMillis, normalizedState.avgUpwardRepDurationMillis, timingWeight),
        avgDownwardRepDurationMillis = weightedAvg(normalizedEntity.avgDownwardRepDurationMillis, normalizedState.avgDownwardRepDurationMillis, timingWeight),
        // Peak force position - characteristic of exercise but can have slight variation
        avgUpwardPeakForcePosition = weightedAvg(normalizedEntity.avgUpwardPeakForcePosition, normalizedState.avgUpwardPeakForcePosition, positionWeight),
        avgDownwardPeakForcePosition = weightedAvg(normalizedEntity.avgDownwardPeakForcePosition, normalizedState.avgDownwardPeakForcePosition, positionWeight),
        // Velocity - somewhat characteristic but varies with load
        avgUpwardMaxVelocity = weightedAvg(normalizedEntity.avgUpwardMaxVelocity, normalizedState.avgUpwardMaxVelocity, timingWeight),
        avgDownwardMaxVelocity = weightedAvg(normalizedEntity.avgDownwardMaxVelocity, normalizedState.avgDownwardMaxVelocity, timingWeight),
        avgRestDurationMillis = weightedAvg(normalizedEntity.avgRestDurationMillis, normalizedState.avgRestDurationMillis, timingWeight)
    )
}

