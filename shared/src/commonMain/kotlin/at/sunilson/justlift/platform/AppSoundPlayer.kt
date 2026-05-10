package at.sunilson.justlift.platform

/**
 * Platform-specific audio playback for workout sounds and TTS.
 */
expect class AppSoundPlayer {
    fun playStart(useTts: Boolean = false)
    fun playAutoStartCountDown()
    fun stopAutoStartCountDown()
    fun playDone(useTts: Boolean = false)
    fun playRep(repNumber: Int, isWarmup: Boolean, useTts: Boolean = false)
    fun playUserSwitch(nextUserId: Int, useTts: Boolean = false)
    fun speak(text: String)
    fun maximizeVolume()
}
