package at.sunilson.justlift.platform

import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice

actual class AppSoundPlayer {
    // Mutable so we can recreate to stop speech immediately, avoiding
    // AVSpeechBoundary type incompatibility across commonized iOS targets.
    private var synthesizer = AVSpeechSynthesizer()

    private fun stopSpeaking() {
        synthesizer = AVSpeechSynthesizer()
    }

    actual fun playStart(useTts: Boolean) {
        if (useTts) {
            speak("Start")
        }
    }

    actual fun playAutoStartCountDown() {
        speak("3, 2, 1")
    }

    actual fun stopAutoStartCountDown() {
        stopSpeaking()
    }

    actual fun playDone(useTts: Boolean) {
        if (useTts) {
            speak("Finish")
        }
    }

    actual fun playRep(repNumber: Int, isWarmup: Boolean, useTts: Boolean) {
        if (useTts) {
            val text = if (isWarmup) "Warmup" else repNumber.toString()
            speak(text)
        }
    }

    actual fun playUserSwitch(nextUserId: Int, useTts: Boolean) {
        speak("Switched to user $nextUserId")
    }

    actual fun speak(text: String) {
        stopSpeaking()
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        utterance.rate = 0.5f
        synthesizer.speakUtterance(utterance)
    }

    actual fun maximizeVolume() {
        // iOS volume is controlled by the system; no programmatic override
        platformLog("AppSoundPlayer", "maximizeVolume - controlled by system on iOS")
    }
}
