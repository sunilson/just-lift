package at.sunilson.justlift.features.workout.data

import android.util.Log
import at.sunilson.justlift.BuildConfig
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryDao
import at.sunilson.justlift.features.workout.data.normalized
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlin.math.roundToInt
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
        NO_EXERCISES_FOR_DIFFICULTY,
        WORKOUT_TOO_SHORT,
        API_ERROR,
        EMPTY_MODEL_RESPONSE,
        MODEL_RESPONSE_NOT_MATCHED,
        INVALID_WORKOUT_DATA
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

        val allExercises = workoutHistoryDao.getAllExercises(userId)
        val existingExercises = allExercises.filter { it.difficulty == workoutState.difficulty.name }

        if (existingExercises.isEmpty()) {
            Log.d("ExerciseRecognition", "recognizeExercise skipped: No existing exercises for user $userId with difficulty ${workoutState.difficulty}")
            return RecognitionResult(null, RecognitionFailureReason.NO_EXERCISES_FOR_DIFFICULTY)
        }

        Log.d("ExerciseRecognition", "Starting recognition with ${existingExercises.size} candidate exercises: ${existingExercises.map { it.name }}")

        val prompt = buildPrompt(workoutState, existingExercises)
        Log.d("ExerciseRecognition", "Recognition Prompt:\n$prompt")

        var attempt = 1
        val maxAttempts = 3
        var lastError: Exception? = null

        while (attempt <= maxAttempts) {
            try {
                val chatCompletionRequest = ChatCompletionRequest(
                    model = ModelId("gpt-5.1"),
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
                val match = existingExercises.find { it.name.equals(result, ignoreCase = true) }?.name

                if (match != null) {
                    Log.d("ExerciseRecognition", "Recognition successful: '$match'")
                    return RecognitionResult(match, null, result)
                } else {
                    // Model returned something, but it didn't match any known exercise
                    Log.w("ExerciseRecognition", "Model response '$result' did not match any known exercise: ${existingExercises.map { it.name }}")
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

    private fun buildPrompt(workout: VitruvianDeviceManager.WorkoutState, existing: List<ExerciseEntity>): String {
        val normalizedWorkout = workout.normalized()
        val workoutData = """
            - Difficulty: ${normalizedWorkout.difficulty}
            - Calibrating Reps: ${normalizedWorkout.calibratingRepsCompleted}
            - Max Reps Target: ${normalizedWorkout.maxReps ?: "Unlimited"}
            - Upward Reps: ${normalizedWorkout.upwardRepetitionsCompleted}
            - Downward Reps: ${normalizedWorkout.downwardRepetitionsCompleted}
            - Time Elapsed: ${normalizedWorkout.timeElapsed.inWholeSeconds}s
            - Avg Upward Force (Total): ${"%.2f".format(normalizedWorkout.averageUpwardForce)}
            - Avg Downward Force (Total): ${"%.2f".format(normalizedWorkout.averageDownwardForce)}
            - Avg Upward Force (Left): ${"%.2f".format(normalizedWorkout.averageUpwardForceLeft)}
            - Avg Upward Force (Right): ${"%.2f".format(normalizedWorkout.averageUpwardForceRight)}
            - Avg Downward Force (Left): ${"%.2f".format(normalizedWorkout.averageDownwardForceLeft)}
            - Avg Downward Force (Right): ${"%.2f".format(normalizedWorkout.averageDownwardForceRight)}
            - Max Upward Force: ${"%.2f".format(normalizedWorkout.maxUpwardForce)}
            - Max Downward Force: ${"%.2f".format(normalizedWorkout.maxDownwardForce)}
            - Avg Upward Rep Duration: ${"%.2f".format(normalizedWorkout.avgUpwardRepDurationMillis / 1000.0)}s
            - Avg Downward Rep Duration: ${"%.2f".format(normalizedWorkout.avgDownwardRepDurationMillis / 1000.0)}s
            - Avg Upward Peak Force Position: ${"%.3f".format(normalizedWorkout.avgUpwardPeakForcePosition)}
            - Avg Downward Peak Force Position: ${"%.3f".format(normalizedWorkout.avgDownwardPeakForcePosition)}
            - Avg Upward Max Velocity: ${"%.3f".format(normalizedWorkout.avgUpwardMaxVelocity)}
            - Avg Downward Max Velocity: ${"%.3f".format(normalizedWorkout.avgDownwardMaxVelocity)}
            - Avg Rest Duration: ${"%.2f".format(normalizedWorkout.avgRestDurationMillis / 1000.0)}s
            - Avg Peak Position Range (Left): ${"%.3f".format(normalizedWorkout.avgMinPositionLeft)} to ${"%.3f".format(normalizedWorkout.avgMaxPositionLeft)}
            - Avg Peak Position Range (Right): ${"%.3f".format(normalizedWorkout.avgMinPositionRight)} to ${"%.3f".format(normalizedWorkout.avgMaxPositionRight)}
            - Absolute Position Range (Left): ${"%.3f".format(normalizedWorkout.minPositionLeft)} to ${"%.3f".format(normalizedWorkout.maxPositionLeft)}
            - Absolute Position Range (Right): ${"%.3f".format(normalizedWorkout.minPositionRight)} to ${"%.3f".format(normalizedWorkout.maxPositionRight)}
        """.trimIndent()

        val exercisesData = existing.map { it.normalized() }.groupBy { it.name }.mapValues { (_, entities) ->
            // Use average of all entries for this exercise name for better fingerprint
            val avgTime = entities.map { it.timeElapsedMillis }.average()
            val avgUpReps = entities.map { it.upwardRepetitionsCompleted }.average()
            val avgDownReps = entities.map { it.downwardRepetitionsCompleted }.average()
            val avgUpForce = entities.map { it.averageUpwardForce }.average()
            val avgDownForce = entities.map { it.averageDownwardForce }.average()
            val avgUpLeft = entities.map { it.averageUpwardForceLeft }.average()
            val avgUpRight = entities.map { it.averageUpwardForceRight }.average()
            val avgDownLeft = entities.map { it.averageDownwardForceLeft }.average()
            val avgDownRight = entities.map { it.averageDownwardForceRight }.average()
            val maxUpForce = entities.map { it.maxUpwardForce }.maxOrNull() ?: 0.0
            val maxDownForce = entities.map { it.maxDownwardForce }.maxOrNull() ?: 0.0
            val avgUpDuration = entities.map { it.avgUpwardRepDurationMillis }.average()
            val avgDownDuration = entities.map { it.avgDownwardRepDurationMillis }.average()
            val avgUpPeakForcePos = entities.map { it.avgUpwardPeakForcePosition }.average()
            val avgDownPeakForcePos = entities.map { it.avgDownwardPeakForcePosition }.average()
            val avgUpMaxVel = entities.map { it.avgUpwardMaxVelocity }.average()
            val avgDownMaxVel = entities.map { it.avgDownwardMaxVelocity }.average()
            val avgRestDur = entities.map { it.avgRestDurationMillis }.average()
            val minPosL = entities.map { it.minPositionLeft }.average()
            val maxPosL = entities.map { it.maxPositionLeft }.average()
            val minPosR = entities.map { it.minPositionRight }.average()
            val maxPosR = entities.map { it.maxPositionRight }.average()
            val avgMinPosL = entities.map { it.avgMinPositionLeft }.average()
            val avgMaxPosL = entities.map { it.avgMaxPositionLeft }.average()
            val avgMinPosR = entities.map { it.avgMinPositionRight }.average()
            val avgMaxPosR = entities.map { it.avgMaxPositionRight }.average()
            
            """
            Exercise: ${entities.first().name} (Difficulty: ${entities.first().difficulty})
            - Avg Time: ${"%.1f".format(avgTime / 1000)}s
            - Avg Reps: ${avgUpReps.roundToInt()} Up / ${avgDownReps.roundToInt()} Down
            - Avg Upward Force (Total): ${"%.2f".format(avgUpForce)}
            - Avg Downward Force (Total): ${"%.2f".format(avgDownForce)}
            - Avg Upward Force (Left): ${"%.2f".format(avgUpLeft)}
            - Avg Upward Force (Right): ${"%.2f".format(avgUpRight)}
            - Avg Downward Force (Left): ${"%.2f".format(avgDownLeft)}
            - Avg Downward Force (Right): ${"%.2f".format(avgDownRight)}
            - Max Upward Force: ${"%.2f".format(maxUpForce)}
            - Max Downward Force: ${"%.2f".format(maxDownForce)}
            - Avg Upward Rep Duration: ${"%.2f".format(avgUpDuration / 1000.0)}s
            - Avg Downward Rep Duration: ${"%.2f".format(avgDownDuration / 1000.0)}s
            - Avg Upward Peak Force Position: ${"%.3f".format(avgUpPeakForcePos)}
            - Avg Downward Peak Force Position: ${"%.3f".format(avgDownPeakForcePos)}
            - Avg Upward Max Velocity: ${"%.3f".format(avgUpMaxVel)}
            - Avg Downward Max Velocity: ${"%.3f".format(avgDownMaxVel)}
            - Avg Rest Duration: ${"%.2f".format(avgRestDur / 1000.0)}s
            - Avg Peak Position Range (Left): ${"%.3f".format(avgMinPosL)} to ${"%.3f".format(avgMaxPosL)}
            - Avg Peak Position Range (Right): ${"%.3f".format(avgMinPosR)} to ${"%.3f".format(avgMaxPosR)}
            - Absolute Position Range (Left): ${"%.3f".format(minPosL)} to ${"%.3f".format(maxPosL)}
            - Absolute Position Range (Right): ${"%.3f".format(minPosR)} to ${"%.3f".format(maxPosR)}
            """.trimIndent()
        }.values.joinToString("\n\n")

        return """
            You are a fitness expert. Based on the provided exercise "fingerprints" (stats), identify which exercise the "Current Workout Data" matches best.
            Note: All provided fingerprints have the same difficulty as the current workout. These fingerprints represent movement patterns for each exercise.

            Existing Exercises Fingerprints:
            $exercisesData

            Current Workout Data:
            $workoutData

            CRITICAL MATCHING RULES (in order of priority):

            1. CABLE USAGE (MUST MATCH):
               - First, determine if one or both cables were used by looking at position ranges.
               - Single-cable exercises are normalized: data appears on "Left" side regardless of actual side.
               - A position range > 0.05 indicates the cable was used.
               - If fingerprint uses 1 cable and workout uses 2 (or vice versa), they are DIFFERENT exercises.

            2. POSITION RANGE (PRIMARY - Most Reliable):
               - "Avg Peak Position Range" is the MOST characteristic metric - it shows the consistent ROM across reps.
               - "Absolute Position Range" shows total ROM including setup/completion movements.
               - These metrics are stable across different weight/volume combinations of the same exercise.
               - Match exercises with similar position ranges first.

            3. PEAK FORCE POSITION (SECONDARY - Movement Pattern):
               - "Avg Upward/Downward Peak Force Position" shows WHERE in the ROM the exercise is hardest.
               - Different exercises have force curves that peak at different positions (bottom vs top).
               - This is relatively stable even when weight/volume changes.

            4. TIMING METRICS (TERTIARY - Tempo Signature):
               - "Avg Upward/Downward Rep Duration" distinguishes tempo variations.
               - "Avg Rest Duration" helps identify pause-style exercises.
               - "Avg Max Velocity" identifies explosive vs controlled movements.

            5. IGNORE THESE FOR MATCHING:
               - Force values (Avg Force, Max Force) vary significantly based on:
                 * Training intensity (high weight/low reps vs low weight/high reps)
                 * Daily strength fluctuations
                 * Progressive overload over time
               - Rep counts vary based on training goals
               - DO NOT let force/rep differences prevent a match if position patterns match.

            6. ALWAYS pick the best matching exercise from the provided fingerprints.
            7. Return ONLY the exercise name, no other text.
        """.trimIndent()
    }
}
