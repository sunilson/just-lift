package at.sunilson.justlift.shared.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import at.sunilson.justlift.R
import org.koin.core.annotation.Single

/**
 * Simple app-wide sound player for short UI sound effects.
 * Uses SoundPool for very short, frequent sounds and falls back to MediaPlayer if a sample
 * hasn't finished loading yet.
 */
@Single
class AppSoundPlayer(private val context: Context) {

    private val soundPool: SoundPool
    private val soundIdByKey: Map<String, Int>
    private val resIdByKey: Map<String, Int>
    private val loadedIds = mutableSetOf<Int>()
    private val playingIds = mutableMapOf<String, Int>()

    init {
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

    fun playStart() = play(KEY_START)
    fun playAutoStartCountDown() = play(KEY_START_COUNTDOWN)
    fun stopAutoStartCountDown() = stop(KEY_START_COUNTDOWN)
    fun playDone() = play(KEY_DONE)
    fun playRepRegular() = play(KEY_REP_REGULAR, volume = 0.9f)

    private fun stop(key: String) {
        val sampleId = soundIdByKey[key]
        if (sampleId != null && loadedIds.contains(sampleId)) {
            Log.d(TAG, "Stopping via SoundPool: $key")
            soundPool.stop(playingIds[key] ?: return)
            return
        }
    }

    private fun play(key: String, volume: Float = 1f) {
        val sampleId = soundIdByKey[key]
        if (sampleId != null && loadedIds.contains(sampleId)) {
            Log.d(TAG, "Playing via SoundPool: $key")
            val id = soundPool.play(sampleId, volume, volume, /*priority*/ 1, /*loop*/ 0, /*rate*/ 1f)
            playingIds[key] = id
            return
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
