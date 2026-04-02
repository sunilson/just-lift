package at.sunilson.justlift.platform

import platform.Foundation.NSLog

actual fun platformLog(tag: String, message: String) {
    NSLog("[$tag] $message")
}
