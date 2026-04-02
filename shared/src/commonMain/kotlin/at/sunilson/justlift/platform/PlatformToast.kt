package at.sunilson.justlift.platform

/**
 * Platform-specific short notification (Toast on Android, no-op/log on iOS).
 */
interface PlatformNotifier {
    fun showShortMessage(message: String)
}
