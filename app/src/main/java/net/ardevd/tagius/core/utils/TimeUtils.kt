package net.ardevd.tagius.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

/**
 * Extension function to convert a Unix timestamp (Seconds) to a time string.
 * Usage: record.t1.toReadableTime() -> "09:00"
 */
fun Long.toReadableTime(): String {
    return timeFormatter.format(Instant.ofEpochSecond(this))
}

/**
 * Extension function to convert a Unix timestamp (Seconds) to a date string.
 * Usage: record.t1.toReadableDate() -> "Mon, Nov 25"
 */
fun Long.toReadableDate(): String {
    return dateFormatter.format(Instant.ofEpochSecond(this))
}

/**
 * Calculates duration between two timestamps in a pretty format.
 * Usage: getDurationString(record.t1, record.t2) -> "1h 30m"
 */
fun getDurationString(start: Long, end: Long): String {
    val durationSeconds = end - start
    if (durationSeconds < 0) return "0m"

    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60

    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}