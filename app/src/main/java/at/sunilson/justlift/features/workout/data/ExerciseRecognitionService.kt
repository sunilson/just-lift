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
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single

@Single
class ExerciseRecognitionService(
    private val workoutHistoryDao: WorkoutHistoryDao
) {
    private val openai = OpenAI(BuildConfig.OPENAI_API_KEY)

    suspend fun recognizeExercise(userId: Int, workoutState: VitruvianDeviceManager.WorkoutState): String? {
        val allExercises = workoutHistoryDao.getAllExercises(userId)
        val existingExercises = allExercises.filter { it.difficulty == workoutState.difficulty.name }
        
        if (existingExercises.isEmpty()) {
            Log.d("ExerciseRecognition", "recognizeExercise skipped: No existing exercises for user $userId with difficulty ${workoutState.difficulty}")
            return null
        }

        val prompt = buildPrompt(workoutState, existingExercises)
        Log.d("ExerciseRecognition", "Recognition Prompt:\n$prompt")

        var attempt = 1
        val maxAttempts = 3
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
                if (result.isNullOrBlank()) return null

                // Verify the result is one of the existing exercise names
                val match = existingExercises.find { it.name.equals(result, ignoreCase = true) }?.name
                Log.d("ExerciseRecognition", "Recognition Final Match: $match")
                return match
            } catch (e: Exception) {
                Log.e("ExerciseRecognition", "Error during exercise recognition (attempt $attempt/$maxAttempts)", e)

                if (attempt < maxAttempts) {
                    delay(2000L * attempt) // Exponential backoff
                    attempt++
                } else {
                    break
                }
            }
        }

        return null
    }

    private fun buildPrompt(workout: VitruvianDeviceManager.WorkoutState, existing: List<ExerciseEntity>): String {
        val workoutData = """
            - Difficulty: ${workout.difficulty}
            - Calibrating Reps: ${workout.calibratingRepsCompleted}
            - Max Reps Target: ${workout.maxReps ?: "Unlimited"}
            - Upward Reps: ${workout.upwardRepetitionsCompleted}
            - Downward Reps: ${workout.downwardRepetitionsCompleted}
            - Time Elapsed: ${workout.timeElapsed.inWholeSeconds}s
            - Avg Upward Force (Total): ${"%.2f".format(workout.averageUpwardForce)}
            - Avg Downward Force (Total): ${"%.2f".format(workout.averageDownwardForce)}
            - Avg Upward Force (Left): ${"%.2f".format(workout.averageUpwardForceLeft)}
            - Avg Upward Force (Right): ${"%.2f".format(workout.averageUpwardForceRight)}
            - Avg Downward Force (Left): ${"%.2f".format(workout.averageDownwardForceLeft)}
            - Avg Downward Force (Right): ${"%.2f".format(workout.averageDownwardForceRight)}
            - Max Upward Force: ${"%.2f".format(workout.maxUpwardForce)}
            - Max Downward Force: ${"%.2f".format(workout.maxDownwardForce)}
            - Avg Upward Rep Duration: ${"%.2f".format(workout.avgUpwardRepDurationMillis / 1000.0)}s
            - Avg Downward Rep Duration: ${"%.2f".format(workout.avgDownwardRepDurationMillis / 1000.0)}s
            - Avg Upward Peak Force Position: ${"%.3f".format(workout.avgUpwardPeakForcePosition)}
            - Avg Downward Peak Force Position: ${"%.3f".format(workout.avgDownwardPeakForcePosition)}
            - Avg Upward Max Velocity: ${"%.3f".format(workout.avgUpwardMaxVelocity)}
            - Avg Downward Max Velocity: ${"%.3f".format(workout.avgDownwardMaxVelocity)}
            - Avg Rest Duration: ${"%.2f".format(workout.avgRestDurationMillis / 1000.0)}s
            - Avg Peak Position Range (Left): ${"%.3f".format(workout.avgMinPositionLeft)} to ${"%.3f".format(workout.avgMaxPositionLeft)}
            - Avg Peak Position Range (Right): ${"%.3f".format(workout.avgMinPositionRight)} to ${"%.3f".format(workout.avgMaxPositionRight)}
            - Absolute Position Range (Left): ${"%.3f".format(workout.minPositionLeft)} to ${"%.3f".format(workout.maxPositionLeft)}
            - Absolute Position Range (Right): ${"%.3f".format(workout.minPositionRight)} to ${"%.3f".format(workout.maxPositionRight)}
        """.trimIndent()

        val exercisesData = existing.groupBy { it.name }.mapValues { (_, entities) ->
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
            Note: All provided fingerprints have the same difficulty as the current workout. These fingerprints represent an average of the user's data for each exercise, so slight variations in form or strength are already factored in.
            
            Existing Exercises Fingerprints:
            $exercisesData
            
            Current Workout Data:
            $workoutData
            
            Instructions:
            1. FIRST, determine if one or both cables were used. Look at the position values (Left/Right) for both the fingerprint and the current data. If a cable's position remains at or near 0, it means it was not used. Match the cable usage (single vs double) before looking at other metrics.
            2. Compare the range of motion (positions), forces (up/down, left/right), rep counts, and timing.
            3. The "Avg Upward Rep Duration" and "Avg Downward Rep Duration" are key for distinguishing exercises with different tempos (e.g., explosive concentric vs. slow eccentric).
            4. The "Avg Upward Peak Force Position" and "Avg Downward Peak Force Position" indicate at what part of the range of motion the most force was applied (e.g., hard at the bottom vs. hard at the top).
            5. The "Avg Upward Max Velocity" and "Avg Downward Max Velocity" help identify explosive or ballistic movements.
            6. The "Avg Rest Duration" can help distinguish exercises that involve pauses at the top or bottom.
            7. Primary Indicator: The "Avg Peak Position Range" is the most characteristic part of an exercise as it averages the peak positions of all completed reps, ignoring startup and shutdown positions.
            8. Secondary Indicator: The "Absolute Position Range" shows the total range reached including the very start and end.
            9. Tertiary Indicator: Forces and rep counts can vary based on daily form and progress.
            10. ALWAYS pick the best matching exercise from the provided fingerprints. DO NOT return "UNKNOWN" or any other text.
            11. Return ONLY the exercise name, no other text.
        """.trimIndent()
    }
}
