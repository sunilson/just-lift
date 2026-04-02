package at.sunilson.justlift.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long =
    (NSDate.date().timeIntervalSince1970 * 1000).toLong()

private fun nsDateFromMillis(timestampMillis: Long): NSDate {
    // NSDate reference date is 2001-01-01, Unix epoch is 1970-01-01
    // Difference is 978307200 seconds
    val timeIntervalSinceRef = (timestampMillis / 1000.0) - 978307200.0
    return NSDate(timeIntervalSinceReferenceDate = timeIntervalSinceRef)
}

actual fun formatTimeShort(timestampMillis: Long): String {
    val date = nsDateFromMillis(timestampMillis)
    val formatter = NSDateFormatter()
    formatter.dateStyle = 0u // NSDateFormatterNoStyle
    formatter.timeStyle = 1u // NSDateFormatterShortStyle
    return formatter.stringFromDate(date)
}

actual fun formatDateShort(timestampMillis: Long): String {
    val date = nsDateFromMillis(timestampMillis)
    val formatter = NSDateFormatter()
    formatter.dateStyle = 2u // NSDateFormatterMediumStyle
    formatter.timeStyle = 0u // NSDateFormatterNoStyle
    return formatter.stringFromDate(date)
}
