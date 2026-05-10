package at.sunilson.justlift.platform

import android.app.Service
import android.content.Context
import android.content.Intent
import org.koin.core.annotation.Single

@Single(binds = [WorkoutServiceController::class])
class AndroidWorkoutServiceController(private val context: Context) : WorkoutServiceController {
    override fun start() {
        val intent = Intent().apply {
            setClassName(context, "at.sunilson.justlift.WorkoutForegroundService")
        }
        try {
            context.startForegroundService(intent)
        } catch (_: Exception) {
            // Ignore if service cannot be started (e.g., app in restricted state)
        }
    }

    override fun stop() {
        val intent = Intent().apply {
            setClassName(context, "at.sunilson.justlift.WorkoutForegroundService")
            action = "at.sunilson.justlift.STOP_WORKOUT_SERVICE"
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
        }
    }
}
