package at.sunilson.justlift.platform

import timber.log.Timber

actual fun platformLog(tag: String, message: String) {
    Timber.tag(tag).d(message)
}
