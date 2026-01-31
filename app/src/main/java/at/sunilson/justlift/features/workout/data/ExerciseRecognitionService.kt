package at.sunilson.justlift.features.workout.data

import android.util.Log
import at.sunilson.justlift.BuildConfig
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryDao
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single

@Single
class ExerciseRecognitionService(
    private val workoutHistoryDao: WorkoutHistoryDao
) {
    private val openai = OpenAI(BuildConfig.OPENAI_API_KEY)

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

    suspend fun recognizeExercise(userId: Int, workoutState: VitruvianDeviceManager.WorkoutState): String? {
        return recognizeExerciseWithReason(userId, workoutState).exerciseName
    }

    suspend fun recognizeExerciseWithReason(userId: Int, workoutState: VitruvianDeviceManager.WorkoutState): RecognitionResult {
        // Validate workout data
        if (!isValidWorkoutData(workoutState)) {
            Log.d("ExerciseRecognition", "recognizeExercise skipped: Invalid workout data (too short or missing position data)")
            return RecognitionResult(null, RecognitionFailureReason.INVALID_WORKOUT_DATA)
        }

        // Get ALL exercises for user (cross-difficulty matching)
        val allExercises = workoutHistoryDao.getAllExercises(userId)

        if (allExercises.isEmpty()) {
            Log.d("ExerciseRecognition", "recognizeExercise skipped: No existing exercises for user $userId")
            return RecognitionResult(null, RecognitionFailureReason.NO_EXERCISES_AVAILABLE)
        }

        // Get unique exercise names (we match by name, not by difficulty)
        val uniqueExerciseNames = allExercises.map { it.name }.distinct()
        Log.d("ExerciseRecognition", "Starting recognition with ${uniqueExerciseNames.size} unique exercises: $uniqueExerciseNames")

        // Build fingerprints
        val workoutFingerprint = buildFingerprint("Current Workout", workoutState.normalized())
        val exerciseFingerprints = buildExerciseFingerprints(allExercises)

        // Filter fingerprints by cable type (must match exactly)
        val matchingCableType = exerciseFingerprints.filter { it.cableType == workoutFingerprint.cableType }

        if (matchingCableType.isEmpty()) {
            Log.d("ExerciseRecognition", "No exercises with matching cable type (${workoutFingerprint.cableType})")
            return RecognitionResult(null, RecognitionFailureReason.NO_EXERCISES_AVAILABLE)
        }

        val prompt = buildPrompt(workoutFingerprint, matchingCableType)
        Log.d("ExerciseRecognition", "Recognition Prompt:\n$prompt")

        var attempt = 1
        val maxAttempts = 3
        var lastError: Exception? = null

        while (attempt <= maxAttempts) {
            try {
                val chatCompletionRequest = ChatCompletionRequest(
                    model = ModelId("gpt-4.1-mini"),
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

                // Verify the result is one of the existing exercise names
                val match = matchingCableType.find { it.name.equals(result, ignoreCase = true) }?.name

                if (match != null) {
                    Log.d("ExerciseRecognition", "Recognition successful: '$match'")
                    return RecognitionResult(match, null, result)
                } else {
                    // Model returned something, but it didn't match any known exercise
                    Log.w("ExerciseRecognition", "Model response '$result' did not match any known exercise: ${matchingCableType.map { it.name }}")
                    return RecognitionResult(null, RecognitionFailureReason.MODEL_RESPONSE_NOT_MATCHED, result)
                }
            } catch (e: Exception) {
                lastError = e
                Log.e("ExerciseRecognition", "Error during exercise recognition (attempt $attempt/$maxAttempts): ${e.message}", e)

                if (attempt < maxAttempts) {
                    delay(2000L * attempt) // Exponential backoff
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
