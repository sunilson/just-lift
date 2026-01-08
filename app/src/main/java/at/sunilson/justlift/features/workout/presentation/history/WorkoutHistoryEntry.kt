package at.sunilson.justlift.features.workout.presentation.history

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryWithStats
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

fun VitruvianDeviceManager.WorkoutState.toExerciseEntity(userId: Int, name: String) = ExerciseEntity(
    userId = userId,
    name = name,
    difficulty = difficulty.name,
    calibratingRepsCompleted = calibratingRepsCompleted,
    maxReps = maxReps,
    upwardRepetitionsCompleted = upwardRepetitionsCompleted,
    downwardRepetitionsCompleted = downwardRepetitionsCompleted,
    timeElapsedMillis = timeElapsed.inWholeMilliseconds,
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
    avgRestDurationMillis = avgRestDurationMillis
)

fun ExerciseEntity.averageWith(workoutState: VitruvianDeviceManager.WorkoutState): ExerciseEntity {
    return copy(
        calibratingRepsCompleted = ((calibratingRepsCompleted + workoutState.calibratingRepsCompleted) / 2.0).roundToInt(),
        maxReps = if (maxReps != null && workoutState.maxReps != null) {
            ((maxReps + workoutState.maxReps) / 2.0).roundToInt()
        } else {
            maxReps ?: workoutState.maxReps
        },
        upwardRepetitionsCompleted = ((upwardRepetitionsCompleted + workoutState.upwardRepetitionsCompleted) / 2.0).roundToInt(),
        downwardRepetitionsCompleted = ((downwardRepetitionsCompleted + workoutState.downwardRepetitionsCompleted) / 2.0).roundToInt(),
        timeElapsedMillis = ((timeElapsedMillis + workoutState.timeElapsed.inWholeMilliseconds) / 2.0).toLong(),
        averageUpwardForce = (averageUpwardForce + workoutState.averageUpwardForce) / 2.0,
        averageDownwardForce = (averageDownwardForce + workoutState.averageDownwardForce) / 2.0,
        maxUpwardForce = (maxUpwardForce + workoutState.maxUpwardForce) / 2.0,
        maxDownwardForce = (maxDownwardForce + workoutState.maxDownwardForce) / 2.0,
        averageUpwardForceLeft = (averageUpwardForceLeft + workoutState.averageUpwardForceLeft) / 2.0,
        averageUpwardForceRight = (averageUpwardForceRight + workoutState.averageUpwardForceRight) / 2.0,
        averageDownwardForceLeft = (averageDownwardForceLeft + workoutState.averageDownwardForceLeft) / 2.0,
        averageDownwardForceRight = (averageDownwardForceRight + workoutState.averageDownwardForceRight) / 2.0,
        minPositionLeft = (minPositionLeft + workoutState.minPositionLeft) / 2.0,
        maxPositionLeft = (maxPositionLeft + workoutState.maxPositionLeft) / 2.0,
        minPositionRight = (minPositionRight + workoutState.minPositionRight) / 2.0,
        maxPositionRight = (maxPositionRight + workoutState.maxPositionRight) / 2.0,
        avgMinPositionLeft = (avgMinPositionLeft + workoutState.avgMinPositionLeft) / 2.0,
        avgMaxPositionLeft = (avgMaxPositionLeft + workoutState.avgMaxPositionLeft) / 2.0,
        avgMinPositionRight = (avgMinPositionRight + workoutState.avgMinPositionRight) / 2.0,
        avgMaxPositionRight = (avgMaxPositionRight + workoutState.avgMaxPositionRight) / 2.0,
        avgUpwardRepDurationMillis = (avgUpwardRepDurationMillis + workoutState.avgUpwardRepDurationMillis) / 2.0,
        avgDownwardRepDurationMillis = (avgDownwardRepDurationMillis + workoutState.avgDownwardRepDurationMillis) / 2.0,
        avgUpwardPeakForcePosition = (avgUpwardPeakForcePosition + workoutState.avgUpwardPeakForcePosition) / 2.0,
        avgDownwardPeakForcePosition = (avgDownwardPeakForcePosition + workoutState.avgDownwardPeakForcePosition) / 2.0,
        avgUpwardMaxVelocity = (avgUpwardMaxVelocity + workoutState.avgUpwardMaxVelocity) / 2.0,
        avgDownwardMaxVelocity = (avgDownwardMaxVelocity + workoutState.avgDownwardMaxVelocity) / 2.0,
        avgRestDurationMillis = (avgRestDurationMillis + workoutState.avgRestDurationMillis) / 2.0
    )
}
