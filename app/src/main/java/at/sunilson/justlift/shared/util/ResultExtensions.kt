package at.sunilson.justlift.shared.util

import java.util.concurrent.CancellationException

@Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
inline fun <T, R> T.runCancellableCatching(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException) {
            throw e
        }

        Result.failure(e)
    }
}
