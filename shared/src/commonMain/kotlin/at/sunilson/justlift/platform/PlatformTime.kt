package at.sunilson.justlift.platform

expect fun currentTimeMillis(): Long

expect fun formatTimeShort(timestampMillis: Long): String

expect fun formatDateShort(timestampMillis: Long): String

/**
 * Returns the epoch millis for [now - duration] where duration is specified by the timeframe.
 */
fun timeframeStartMillis(
    nowMillis: Long,
    weeks: Int = 0,
    months: Int = 0,
    years: Int = 0
): Long {
    val msPerWeek = 7L * 24 * 60 * 60 * 1000
    val msPerMonth = 30L * 24 * 60 * 60 * 1000 // approximate
    val msPerYear = 365L * 24 * 60 * 60 * 1000 // approximate
    return nowMillis - (weeks * msPerWeek) - (months * msPerMonth) - (years * msPerYear)
}
