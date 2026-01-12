package at.sunilson.justlift.shared.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.util.Log
import at.sunilson.justlift.R
import org.koin.core.annotation.Single
import java.util.Locale

/**
 * Simple app-wide sound player for short UI sound effects.
 * Uses SoundPool for very short, frequent sounds and TextToSpeech for verbal feedback.
 */
@Single
class AppSoundPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    private val soundPool: SoundPool
    private val soundIdByKey: Map<String, Int>
    private val resIdByKey: Map<String, Int>
    private val loadedIds = mutableSetOf<Int>()
    private val playingIds = mutableMapOf<String, Int>()

    private var tts: TextToSpeech? = null
    private var ttsInitialized = false

    init {
        tts = TextToSpeech(context, this)

        val attrs = AudioAttributes.Builder()
            // Use media usage so playback follows media volume rather than ringer/notification
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        resIdByKey = mapOf(
            KEY_START to R.raw.start,
            KEY_DONE to R.raw.done,
            KEY_REP_REGULAR to R.raw.rep_regular,
            KEY_START_COUNTDOWN to R.raw.countdown_three_sec
        )

        soundIdByKey = resIdByKey.mapValues { (_, res) -> soundPool.load(context, res, 1) }

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedIds += sampleId
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            ttsInitialized = true
        }
    }

    fun speak(text: String) {
        if (ttsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun playStart(useTts: Boolean = false) {
        if (useTts) speak("Start") else play(KEY_START)
    }

    fun playAutoStartCountDown() = play(KEY_START_COUNTDOWN)

    fun stopAutoStartCountDown() = stop(KEY_START_COUNTDOWN)

    fun playDone(useTts: Boolean = false) {
        if (useTts) speak("Finish") else play(KEY_DONE)
    }

    fun playRep(repNumber: Int, isWarmup: Boolean, useTts: Boolean = false) {
        if (useTts) {
            val text = if (isWarmup) "Warmup" else repNumber.toString()
            speak(text)
        } else {
            play(KEY_REP_REGULAR, volume = 0.9f)
        }
    }

    fun maximizeVolume() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }

    private fun stop(key: String) {
        val streamId = playingIds.remove(key)
        if (streamId != null && streamId != 0) {
            Log.d(TAG, "Stopping via SoundPool: $key (streamId=$streamId)")
            soundPool.stop(streamId)
        } else {
            Log.d(TAG, "Stop requested but no active stream for: $key")
        }
    }

    private fun play(key: String, volume: Float = 1f) {
        val sampleId = soundIdByKey[key]
        if (sampleId != null && loadedIds.contains(sampleId)) {
            Log.d(TAG, "Playing via SoundPool: $key")
            val id = soundPool.play(sampleId, volume, volume, /*priority*/ 1, /*loop*/ 0, /*rate*/ 1f)
            if (id != 0) {
                playingIds[key] = id
            }
        } else {
            Log.d(TAG, "Play requested before sample loaded: $key")
        }
    }

    companion object {
        private const val TAG = "AppSoundPlayer"
        private const val KEY_START_COUNTDOWN = "startCountdown"
        private const val KEY_START = "start"
        private const val KEY_DONE = "done"
        private const val KEY_REP_REGULAR = "rep_regular"
    }
}
