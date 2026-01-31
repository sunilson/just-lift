package at.sunilson.justlift

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.sunilson.justlift.shared.audio.AppSoundPlayer
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()
    private val appSoundPlayer: AppSoundPlayer by inject()
    private var isReady = false

    override fun onResume() {
        super.onResume()
        appSoundPlayer.maximizeVolume()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Force dark status bar icons whenever window gains focus
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep splash screen visible while app initializes
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Add exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Fade out animation
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f,
                0f
            ).apply {
                duration = 400L
                interpolator = AccelerateDecelerateInterpolator()
                doOnEnd { splashScreenView.remove() }
            }

            // Scale up animation for dramatic effect
            val scaleX = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.SCALE_X,
                1f,
                1.5f
            ).apply {
                duration = 400L
                interpolator = AccelerateDecelerateInterpolator()
            }

            val scaleY = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.SCALE_Y,
                1f,
                1.5f
            ).apply {
                duration = 400L
                interpolator = AccelerateDecelerateInterpolator()
            }

            // Start all animations together
            fadeOut.start()
            scaleX.start()
            scaleY.start()
        }

        // Use light style for status bar to get dark icons
        // The scrim color tells the system the bar is "light", so use dark icons
        // We use a nearly transparent white to maintain edge-to-edge while signaling light theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            JustLiftTheme {
                JustLiftApp()
            }
        }

        // Mark app as ready after a brief delay to ensure smooth animation
        window.decorView.post {
            isReady = true
        }
    }
}
