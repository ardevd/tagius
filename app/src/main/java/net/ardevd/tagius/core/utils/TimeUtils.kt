package net.ardevd.tagius.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale


object DateRanges {
    fun getToday(): Pair<Long, Long> {
        val now = LocalDate.now()
        return getDayRange(now)
    }

    fun getLast7Days(): Pair<Long, Long> {
        val end = LocalDate.now()
        val start = end.minusDays(6) // 7 days inclusive
        return getRange(start, end)
    }

    fun getThisMonth(): Pair<Long, Long> {
        val now = LocalDate.now()
        val start = now.with(TemporalAdjusters.firstDayOfMonth())
        val end = now.with(TemporalAdjusters.lastDayOfMonth())
        return getRange(start, end)
    }

    // Helper to convert LocalDate to Unix Seconds (Start of day -> End of day)
    private fun getDayRange(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toEpochSecond()
        val end = date.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        return Pair(start, end)
    }

    private fun getRange(start: LocalDate, end: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val t1 = start.atStartOfDay(zone).toEpochSecond()
        val t2 = end.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        return Pair(t1, t2)
    }
}
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