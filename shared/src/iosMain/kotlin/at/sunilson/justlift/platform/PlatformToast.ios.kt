package at.sunilson.justlift.platform

class IosNotifier : PlatformNotifier {
    override fun showShortMessage(message: String) {
        platformLog("Notification", message)
    }
}
