package at.sunilson.justlift.platform

/**
 * Platform-specific context wrapper.
 * On Android this wraps android.content.Context.
 * On iOS this is an empty object (no context needed).
 */
expect class PlatformContext
