package at.sunilson.justlift.platform

import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * KMP-compatible replacement for "%.0f".format(value)
 */
fun Double.formatNoDecimals(): String = roundToLong().toString()

fun Float.formatNoDecimals(): String = roundToInt().toString()

/**
 * KMP-compatible replacement for "%.2f".format(value)
 */
fun Float.formatTwoDecimals(): String {
    val intPart = toLong()
    val fracPart = ((this - intPart).absoluteValue * 100).roundToInt()
    val sign = if (this < 0 && intPart == 0L) "-" else ""
    return "$sign$intPart.${fracPart.toString().padStart(2, '0')}"
}
