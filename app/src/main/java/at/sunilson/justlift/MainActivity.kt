package at.sunilson.justlift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import at.sunilson.justlift.shared.audio.AppSoundPlayer
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()
    private val appSoundPlayer: AppSoundPlayer by inject()

    override fun onResume() {
        super.onResume()
        appSoundPlayer.maximizeVolume()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JustLiftTheme {
                JustLiftApp()
            }
        }
    }
}
