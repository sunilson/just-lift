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
            - Avg Peak Position Range (Left): ${"%.3f".format(avgMinPosL)} to ${"%.3f".format(avgMaxPosL)}
            - Avg Peak Position Range (Right): ${"%.3f".format(avgMinPosR)} to ${"%.3f".format(avgMaxPosR)}
            - Absolute Position Range (Left): ${"%.3f".format(minPosL)} to ${"%.3f".format(maxPosL)}
            - Absolute Position Range (Right): ${"%.3f".format(minPosR)} to ${"%.3f".format(maxPosR)}
            """.trimIndent()
        }.values.joinToString("\n\n")

        return """
            You are a fitness expert. Based on the provided exercise "fingerprints" (stats), identify which exercise the "Current Workout Data" matches best.
            Note: All provided fingerprints have the same difficulty as the current workout.
            
            Existing Exercises Fingerprints:
            $exercisesData
            
            Current Workout Data:
            $workoutData
            
            Instructions:
            1. Compare the range of motion (positions), forces (up/down, left/right), rep counts, and timing.
            2. Primary Indicator: The "Avg Peak Position Range" is the most characteristic part of an exercise as it averages the peak positions of all completed reps, ignoring startup and shutdown positions.
            3. Secondary Indicator: The "Absolute Position Range" shows the total range reached including the very start and end.
            4. Tertiary Indicator: Forces and rep counts can vary based on daily form and progress.
            5. ALWAYS pick the best matching exercise from the provided fingerprints. DO NOT return "UNKNOWN" or any other text.
            6. Return ONLY the exercise name, no other text.
        """.trimIndent()
    }
}
