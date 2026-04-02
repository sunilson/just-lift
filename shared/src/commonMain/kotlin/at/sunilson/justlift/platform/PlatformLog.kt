package at.sunilson.justlift.platform

/**
 * Platform-specific logging.
 * On Android uses Timber, on iOS uses NSLog.
 */
expect fun platformLog(tag: String, message: String)
