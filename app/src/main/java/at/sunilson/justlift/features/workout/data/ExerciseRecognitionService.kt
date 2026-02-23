package at.sunilson.justlift.features.workout.data

import android.util.Log
import at.sunilson.justlift.BuildConfig
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.database.ExerciseSampleEntity
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryDao
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import org.koin.core.annotation.Single

@Single
class ExerciseRecognitionService(
    private val workoutHistoryDao: WorkoutHistoryDao
) {
    private val openai = OpenAI(
        OpenAIConfig(
            token = BuildConfig.OPENAI_API_KEY,
            timeout = Timeout(socket = 60.seconds, request = 60.seconds)
        )
    )

    /**
     * Recognition result with additional context for debugging and future improvements.
     */
    data class RecognitionResult(
        val exerciseName: String?,
        val reason: RecognitionFailureReason? = null,
        val rawModelResponse: String? = null
    )

    enum class RecognitionFailureReason {
        NO_EXERCISES_AVAILABLE,
        WORKOUT_TOO_SHORT,
        API_ERROR,
        EMPTY_MODEL_RESPONSE,
        MODEL_RESPONSE_NOT_MATCHED,
        INVALID_WORKOUT_DATA
    }

    /**
     * Movement fingerprint containing only the metrics that identify an exercise
     * independent of weight, reps, difficulty, or which cable side is used.
     */
    private data class MovementFingerprint(
        val name: String,
        val cableType: CableType,
        val positionRangeLeft: Double,
        val positionRangeRight: Double,
        val avgPositionRangeLeft: Double,
        val avgPositionRangeRight: Double,
        val peakForcePositionUp: Double,
        val peakForcePositionDown: Double,
        val movementRatio: Double, // upward duration / downward duration
        val avgVelocityUp: Double,
        val avgVelocityDown: Double
    )

    private enum class CableType {
        SINGLE, DUAL
    }

    private data class MetricStdDevs(
        val positionRangeLeft: Double = 0.0,
        val positionRangeRight: Double = 0.0,
        val avgPositionRangeLeft: Double = 0.0,
        val avgPositionRangeRight: Double = 0.0,
        val peakForcePositionUp: Double = 0.0,
        val peakForcePositionDown: Double = 0.0,
        val movementRatio: Double = 0.0,
        val avgVelocityUp: Double = 0.0,
        val avgVelocityDown: Double = 0.0
    )

    private data class EnrichedFingerprint(
        val fingerprint: MovementFingerprint,
        val sampleCount: Int,
        val stdDevs: MetricStdDevs
    )

    suspend fun recognizeExercise(userId: Int, workoutState: VitruvianDeviceManager.WorkoutState): String? {
        return recognizeExerciseWithReason(userId, workoutState).exerciseName
    }

    suspend fun recognizeExerciseWithReason(userId: Int, workoutState: VitruvianDeviceManager.WorkoutState): RecognitionResult {
        // Validate workout data
        if (!isValidWorkoutData(workoutState)) {
            Log.d("ExerciseRecognition", "recognizeExercise skipped: Invalid workout data (too short or missing position data)")
            return RecognitionResult(null, RecognitionFailureReason.INVALID_WORKOUT_DATA)
        }

        val allSamples = workoutHistoryDao.getAllExerciseSamples(userId)

        if (allSamples.isEmpty()) {
            Log.d("ExerciseRecognition", "recognizeExercise skipped: No exercise samples for user $userId")
            return RecognitionResult(null, RecognitionFailureReason.NO_EXERCISES_AVAILABLE)
        }

        val nowMillis = System.currentTimeMillis()
        val enrichedFingerprints = buildFingerprintsFromSamples(allSamples, nowMillis)

        if (enrichedFingerprints.isEmpty()) {
            Log.d("ExerciseRecognition", "recognizeExercise skipped: No valid fingerprints after filtering")
            return RecognitionResult(null, RecognitionFailureReason.NO_EXERCISES_AVAILABLE)
        }

        val uniqueNames = enrichedFingerprints.map { it.fingerprint.name }
        Log.d("ExerciseRecognition", "Starting recognition with ${uniqueNames.size} exercises: $uniqueNames")

        val workoutFingerprint = buildFingerprint("Current Workout", workoutState.normalized())

        // Filter by cable type
        val matchingCableType = enrichedFingerprints.filter { it.fingerprint.cableType == workoutFingerprint.cableType }

        if (matchingCableType.isEmpty()) {
            Log.d("ExerciseRecognition", "No exercises with matching cable type (${workoutFingerprint.cableType})")
            return RecognitionResult(null, RecognitionFailureReason.NO_EXERCISES_AVAILABLE)
        }

        val prompt = buildEnrichedPrompt(workoutFingerprint, matchingCableType)
        Log.d("ExerciseRecognition", "Recognition Prompt:\n$prompt")

        var attempt = 1
        val maxAttempts = 3
        var lastError: Exception? = null

        while (attempt <= maxAttempts) {
            try {
                val chatCompletionRequest = ChatCompletionRequest(
                    model = ModelId("gpt-5-mini"),
                    messages = listOf(
                        ChatMessage(
                            role = ChatRole.User,
                            content = prompt
                        )
                    )
                )
                val completion = openai.chatCompletion(chatCompletionRequest)
                val result = completion.choices.firstOrNull()?.message?.content?.trim()
                Log.d("ExerciseRecognition", "Model Raw Response: $result")

                if (result.isNullOrBlank()) {
                    Log.w("ExerciseRecognition", "Model returned empty/blank response")
                    return RecognitionResult(null, RecognitionFailureReason.EMPTY_MODEL_RESPONSE, result)
                }

                val match = matchingCableType.find { it.fingerprint.name.equals(result, ignoreCase = true) }?.fingerprint?.name

                if (match != null) {
                    Log.d("ExerciseRecognition", "Recognition successful: '$match'")
                    return RecognitionResult(match, null, result)
                } else {
                    Log.w("ExerciseRecognition", "Model response '$result' did not match any known exercise: ${matchingCableType.map { it.fingerprint.name }}")
                    return RecognitionResult(null, RecognitionFailureReason.MODEL_RESPONSE_NOT_MATCHED, result)
                }
            } catch (e: Exception) {
                lastError = e
                Log.e("ExerciseRecognition", "Error during exercise recognition (attempt $attempt/$maxAttempts): ${e.message}", e)

                if (attempt < maxAttempts) {
                    delay(2000L * attempt)
                    attempt++
                } else {
                    break
                }
            }
        }

        Log.e("ExerciseRecognition", "Recognition failed after $maxAttempts attempts", lastError)
        return RecognitionResult(null, RecognitionFailureReason.API_ERROR)
    }

    /**
     * Validates that the workout data has enough information for meaningful recognition.
     */
    private fun isValidWorkoutData(workoutState: VitruvianDeviceManager.WorkoutState): Boolean {
        // Check if workout has at least 1 completed rep
        if (workoutState.upwardRepetitionsCompleted < 1 && workoutState.downwardRepetitionsCompleted < 1) {
            Log.d("ExerciseRecognition", "Validation failed: No completed reps")
            return false
        }

        // Check if there's meaningful position data (at least one cable has movement)
        val leftRange = workoutState.maxPositionLeft - workoutState.minPositionLeft
        val rightRange = workoutState.maxPositionRight - workoutState.minPositionRight

        if (leftRange <= 0.05 && rightRange <= 0.05) {
            Log.d("ExerciseRecognition", "Validation failed: No meaningful position data (leftRange=$leftRange, rightRange=$rightRange)")
            return false
        }

        return true
    }

    /**
     * Builds a movement fingerprint from workout state.
     */
    private fun buildFingerprint(name: String, state: VitruvianDeviceManager.WorkoutState): MovementFingerprint {
        val leftRange = state.maxPositionLeft - state.minPositionLeft
        val rightRange = state.maxPositionRight - state.minPositionRight
        val avgLeftRange = state.avgMaxPositionLeft - state.avgMinPositionLeft
        val avgRightRange = state.avgMaxPositionRight - state.avgMinPositionRight

        val cableType = if (leftRange > 0.05 && rightRange > 0.05) CableType.DUAL else CableType.SINGLE

        val movementRatio = if (state.avgDownwardRepDurationMillis > 0) {
            state.avgUpwardRepDurationMillis / state.avgDownwardRepDurationMillis
        } else 1.0

        return MovementFingerprint(
            name = name,
            cableType = cableType,
            positionRangeLeft = leftRange,
            positionRangeRight = rightRange,
            avgPositionRangeLeft = avgLeftRange,
            avgPositionRangeRight = avgRightRange,
            peakForcePositionUp = state.avgUpwardPeakForcePosition,
            peakForcePositionDown = state.avgDownwardPeakForcePosition,
            movementRatio = movementRatio,
            avgVelocityUp = state.avgUpwardMaxVelocity,
            avgVelocityDown = state.avgDownwardMaxVelocity
        )
    }

    /**
     * Builds enriched fingerprints from exercise samples using time-decayed weighted averaging.
     * Groups by exerciseName (pools across all difficulties), takes most recent 30 samples per exercise,
     * and computes per-metric standard deviations.
     */
    private fun buildFingerprintsFromSamples(
        samples: List<ExerciseSampleEntity>,
        nowMillis: Long
    ): List<EnrichedFingerprint> {
        val halfLifeMillis = 30.0 * 24 * 60 * 60 * 1000 // 30 days
        val decayRate = ln(2.0) / halfLifeMillis

        return samples
            .map { it.normalized() }
            .filter { sample ->
                val leftRange = sample.maxPositionLeft - sample.minPositionLeft
                val rightRange = sample.maxPositionRight - sample.minPositionRight
                leftRange > 0.01 || rightRange > 0.01
            }
            .groupBy { it.exerciseName }
            .mapNotNull { (name, exerciseSamples) ->
                val recent = exerciseSamples
                    .sortedByDescending { it.timestampMillis }
                    .take(30)

                if (recent.isEmpty()) return@mapNotNull null

                // Compute time-decay weights
                val weights = recent.map { sample ->
                    val ageMillis = (nowMillis - sample.timestampMillis).toDouble().coerceAtLeast(0.0)
                    exp(-decayRate * ageMillis)
                }
                val totalWeight = weights.sum()
                if (totalWeight <= 0) return@mapNotNull null

                // Extract per-sample metric values
                val leftRanges = recent.map { it.maxPositionLeft - it.minPositionLeft }
                val rightRanges = recent.map { it.maxPositionRight - it.minPositionRight }
                val avgLeftRanges = recent.map { it.avgMaxPositionLeft - it.avgMinPositionLeft }
                val avgRightRanges = recent.map { it.avgMaxPositionRight - it.avgMinPositionRight }
                val peakUps = recent.map { it.avgUpwardPeakForcePosition }
                val peakDowns = recent.map { it.avgDownwardPeakForcePosition }
                val velUps = recent.map { it.avgUpwardMaxVelocity }
                val velDowns = recent.map { it.avgDownwardMaxVelocity }
                val movementRatios = recent.map { sample ->
                    if (sample.avgDownwardRepDurationMillis > 0) {
                        sample.avgUpwardRepDurationMillis / sample.avgDownwardRepDurationMillis
                    } else 1.0
                }

                fun weightedAvg(values: List<Double>): Double =
                    values.zip(weights).sumOf { (v, w) -> v * w } / totalWeight

                fun weightedStdDev(values: List<Double>, mean: Double): Double {
                    if (values.size < 2) return 0.0
                    val variance = values.zip(weights).sumOf { (v, w) -> w * (v - mean) * (v - mean) } / totalWeight
                    return sqrt(variance)
                }

                val avgLeftRange = weightedAvg(leftRanges)
                val avgRightRange = weightedAvg(rightRanges)
                val avgAvgLeftRange = weightedAvg(avgLeftRanges)
                val avgAvgRightRange = weightedAvg(avgRightRanges)
                val avgPeakUp = weightedAvg(peakUps)
                val avgPeakDown = weightedAvg(peakDowns)
                val avgVelUp = weightedAvg(velUps)
                val avgVelDown = weightedAvg(velDowns)
                val avgMovementRatio = weightedAvg(movementRatios)

                val cableType = if (avgLeftRange > 0.05 && avgRightRange > 0.05) CableType.DUAL else CableType.SINGLE

                val fingerprint = MovementFingerprint(
                    name = name,
                    cableType = cableType,
                    positionRangeLeft = avgLeftRange,
                    positionRangeRight = avgRightRange,
                    avgPositionRangeLeft = avgAvgLeftRange,
                    avgPositionRangeRight = avgAvgRightRange,
                    peakForcePositionUp = avgPeakUp,
                    peakForcePositionDown = avgPeakDown,
                    movementRatio = avgMovementRatio,
                    avgVelocityUp = avgVelUp,
                    avgVelocityDown = avgVelDown
                )

                val stdDevs = MetricStdDevs(
                    positionRangeLeft = weightedStdDev(leftRanges, avgLeftRange),
                    positionRangeRight = weightedStdDev(rightRanges, avgRightRange),
                    avgPositionRangeLeft = weightedStdDev(avgLeftRanges, avgAvgLeftRange),
                    avgPositionRangeRight = weightedStdDev(avgRightRanges, avgAvgRightRange),
                    peakForcePositionUp = weightedStdDev(peakUps, avgPeakUp),
                    peakForcePositionDown = weightedStdDev(peakDowns, avgPeakDown),
                    movementRatio = weightedStdDev(movementRatios, avgMovementRatio),
                    avgVelocityUp = weightedStdDev(velUps, avgVelUp),
                    avgVelocityDown = weightedStdDev(velDowns, avgVelDown)
                )

                EnrichedFingerprint(fingerprint, recent.size, stdDevs)
            }
    }

    private fun buildEnrichedPrompt(workout: MovementFingerprint, candidates: List<EnrichedFingerprint>): String {
        val workoutData = formatFingerprint(workout)
        val exercisesData = candidates.joinToString("\n\n") { enriched ->
            val fp = enriched.fingerprint
            val sd = enriched.stdDevs
            """
            ${fp.name} (N=${enriched.sampleCount} samples):
            - Cable Type: ${fp.cableType}
            - Position Range (Left): ${"%.3f".format(fp.positionRangeLeft)} (StdDev: ${"%.3f".format(sd.positionRangeLeft)})
            - Position Range (Right): ${"%.3f".format(fp.positionRangeRight)} (StdDev: ${"%.3f".format(sd.positionRangeRight)})
            - Avg Position Range (Left): ${"%.3f".format(fp.avgPositionRangeLeft)} (StdDev: ${"%.3f".format(sd.avgPositionRangeLeft)})
            - Avg Position Range (Right): ${"%.3f".format(fp.avgPositionRangeRight)} (StdDev: ${"%.3f".format(sd.avgPositionRangeRight)})
            - Peak Force Position (Up): ${"%.3f".format(fp.peakForcePositionUp)} (StdDev: ${"%.3f".format(sd.peakForcePositionUp)})
            - Peak Force Position (Down): ${"%.3f".format(fp.peakForcePositionDown)} (StdDev: ${"%.3f".format(sd.peakForcePositionDown)})
            - Movement Ratio (Up/Down): ${"%.2f".format(fp.movementRatio)} (StdDev: ${"%.2f".format(sd.movementRatio)})
            - Avg Velocity (Up): ${"%.3f".format(fp.avgVelocityUp)} (StdDev: ${"%.3f".format(sd.avgVelocityUp)})
            - Avg Velocity (Down): ${"%.3f".format(fp.avgVelocityDown)} (StdDev: ${"%.3f".format(sd.avgVelocityDown)})
            """.trimIndent()
        }

        return """
            You are an exercise recognition system. Match the current workout to the most similar exercise based on movement pattern.

            AVAILABLE EXERCISES:
            $exercisesData

            CURRENT WORKOUT:
            $workoutData

            MATCHING RULES (in priority order):

            1. POSITION RANGE (ROM) - PRIMARY METRIC:
               - Compare "Position Range" values (the total movement distance)
               - Compare "Avg Position Range" values (the consistent ROM across reps)
               - Same exercise will have similar ROM values (within ~0.1 tolerance)
               - This is the MOST RELIABLE metric - it doesn't change with weight or difficulty

            2. PEAK FORCE POSITION - SECONDARY METRIC:
               - Where in the movement is the exercise hardest (0.0 = bottom, 1.0 = top)
               - Different exercises have different force curves
               - Should be within ~0.15 tolerance for a match

            3. MOVEMENT RATIO - TERTIARY METRIC:
               - Ratio of concentric (up) to eccentric (down) duration
               - Helps distinguish tempo variations
               - Less reliable but useful for tie-breaking

            4. VELOCITY - QUATERNARY METRIC:
               - Average max velocity during up/down phases
               - Distinguishes explosive vs controlled movements

            RELIABILITY HINTS:
            - Each metric includes a StdDev (standard deviation). LOW StdDev = very reliable metric for this exercise, HIGH StdDev = less reliable, weigh it less.
            - Exercises with more samples (higher N) have more reliable fingerprints overall.
            - When two exercises are close, prefer the one where the matching metrics have lower StdDev.

            IMPORTANT:
            - All exercises shown have the same cable type (${workout.cableType}) as the workout
            - Pick the BEST match even if not perfect
            - Return ONLY the exercise name, nothing else
        """.trimIndent()
    }

    /**
     * Builds fingerprints for all exercises, averaging across difficulties.
     * This enables cross-difficulty matching since the same exercise has similar ROM
     * regardless of the difficulty setting.
     *
     * Filters out legacy fingerprints that have no position data (from before
     * position tracking was added in earlier app versions).
     */
    private fun buildExerciseFingerprints(exercises: List<ExerciseEntity>): List<MovementFingerprint> {
        return exercises
            .map { it.normalized() }
            // Filter out legacy fingerprints with no position data
            .filter { entity ->
                val leftRange = entity.maxPositionLeft - entity.minPositionLeft
                val rightRange = entity.maxPositionRight - entity.minPositionRight
                leftRange > 0.01 || rightRange > 0.01
            }
            .groupBy { it.name }
            .map { (name, entities) ->
                // Average position-based metrics across all difficulties for this exercise
                val avgLeftRange = entities.map { it.maxPositionLeft - it.minPositionLeft }.average()
                val avgRightRange = entities.map { it.maxPositionRight - it.minPositionRight }.average()
                val avgAvgLeftRange = entities.map { it.avgMaxPositionLeft - it.avgMinPositionLeft }.average()
                val avgAvgRightRange = entities.map { it.avgMaxPositionRight - it.avgMinPositionRight }.average()
                val avgPeakUp = entities.map { it.avgUpwardPeakForcePosition }.average()
                val avgPeakDown = entities.map { it.avgDownwardPeakForcePosition }.average()
                val avgVelUp = entities.map { it.avgUpwardMaxVelocity }.average()
                val avgVelDown = entities.map { it.avgDownwardMaxVelocity }.average()

                val movementRatios = entities.mapNotNull { entity ->
                    if (entity.avgDownwardRepDurationMillis > 0) {
                        entity.avgUpwardRepDurationMillis / entity.avgDownwardRepDurationMillis
                    } else null
                }
                val avgMovementRatio = if (movementRatios.isNotEmpty()) movementRatios.average() else 1.0

                val cableType = if (avgLeftRange > 0.05 && avgRightRange > 0.05) CableType.DUAL else CableType.SINGLE

                MovementFingerprint(
                    name = name,
                    cableType = cableType,
                    positionRangeLeft = avgLeftRange,
                    positionRangeRight = avgRightRange,
                    avgPositionRangeLeft = avgAvgLeftRange,
                    avgPositionRangeRight = avgAvgRightRange,
                    peakForcePositionUp = avgPeakUp,
                    peakForcePositionDown = avgPeakDown,
                    movementRatio = avgMovementRatio,
                    avgVelocityUp = avgVelUp,
                    avgVelocityDown = avgVelDown
                )
            }
    }

    private fun buildPrompt(workout: MovementFingerprint, candidates: List<MovementFingerprint>): String {
        val workoutData = formatFingerprint(workout)
        val exercisesData = candidates.joinToString("\n\n") { formatFingerprint(it) }

        return """
            You are an exercise recognition system. Match the current workout to the most similar exercise based on movement pattern.

            AVAILABLE EXERCISES:
            $exercisesData

            CURRENT WORKOUT:
            $workoutData

            MATCHING RULES (in priority order):

            1. POSITION RANGE (ROM) - PRIMARY METRIC:
               - Compare "Position Range" values (the total movement distance)
               - Compare "Avg Position Range" values (the consistent ROM across reps)
               - Same exercise will have similar ROM values (within ~0.1 tolerance)
               - This is the MOST RELIABLE metric - it doesn't change with weight or difficulty

            2. PEAK FORCE POSITION - SECONDARY METRIC:
               - Where in the movement is the exercise hardest (0.0 = bottom, 1.0 = top)
               - Different exercises have different force curves
               - Should be within ~0.15 tolerance for a match

            3. MOVEMENT RATIO - TERTIARY METRIC:
               - Ratio of concentric (up) to eccentric (down) duration
               - Helps distinguish tempo variations
               - Less reliable but useful for tie-breaking

            4. VELOCITY - QUATERNARY METRIC:
               - Average max velocity during up/down phases
               - Distinguishes explosive vs controlled movements

            IMPORTANT:
            - All exercises shown have the same cable type (${workout.cableType}) as the workout
            - Pick the BEST match even if not perfect
            - Return ONLY the exercise name, nothing else
        """.trimIndent()
    }

    private fun formatFingerprint(fp: MovementFingerprint): String {
        return """
            ${fp.name}:
            - Cable Type: ${fp.cableType}
            - Position Range (Left): ${"%.3f".format(fp.positionRangeLeft)}
            - Position Range (Right): ${"%.3f".format(fp.positionRangeRight)}
            - Avg Position Range (Left): ${"%.3f".format(fp.avgPositionRangeLeft)}
            - Avg Position Range (Right): ${"%.3f".format(fp.avgPositionRangeRight)}
            - Peak Force Position (Up): ${"%.3f".format(fp.peakForcePositionUp)}
            - Peak Force Position (Down): ${"%.3f".format(fp.peakForcePositionDown)}
            - Movement Ratio (Up/Down): ${"%.2f".format(fp.movementRatio)}
            - Avg Velocity (Up): ${"%.3f".format(fp.avgVelocityUp)}
            - Avg Velocity (Down): ${"%.3f".format(fp.avgVelocityDown)}
        """.trimIndent()
    }
}
