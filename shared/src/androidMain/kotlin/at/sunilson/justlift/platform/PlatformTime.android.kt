package at.sunilson.justlift.platform

import java.text.DateFormat
import java.util.Date

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTimeShort(timestampMillis: Long): String {
    val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
    return fmt.format(Date(timestampMillis))
}

actual fun formatDateShort(timestampMillis: Long): String {
    val fmt = DateFormat.getDateInstance(DateFormat.DEFAULT)
    return fmt.format(Date(timestampMillis))
}
