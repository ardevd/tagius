package net.ardevd.tagius.core.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Unit tests for DateRanges object in TimeUtils.kt
 * 
 * These tests verify that date range calculations are correct for:
 * - Today's date range
 * - Last 7 days date range
 * - Current month date range
 * - Edge cases like month boundaries
 */
class TimeUtilsTest {

    @Test
    fun getToday_returnsCorrectStartAndEndTimestamps() {
        // Given: Today's date
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        
        // When: Getting today's range
        val (start, end) = DateRanges.getToday()
        
        // Then: Start should be beginning of today
        val expectedStart = today.atStartOfDay(zone).toEpochSecond()
        assertEquals(expectedStart, start)
        
        // And: End should be last second of today (one second before tomorrow)
        val expectedEnd = today.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        assertEquals(expectedEnd, end)
        
        // And: The range should cover exactly 24 hours minus 1 second
        val durationSeconds = end - start + 1 // +1 because both endpoints are inclusive
        assertEquals(86400L, durationSeconds) // 24 * 60 * 60 = 86400 seconds
    }

    @Test
    fun getToday_startTimestampIsBeforeEndTimestamp() {
        // When: Getting today's range
        val (start, end) = DateRanges.getToday()
        
        // Then: Start should always be before end
        assertTrue("Start timestamp should be less than end timestamp", start < end)
    }

    @Test
    fun getLast7Days_correctlyCalculates7DayRange() {
        // Given: Today's date and 6 days ago (7 days inclusive)
        val today = LocalDate.now()
        val sixDaysAgo = today.minusDays(6)
        val zone = ZoneId.systemDefault()
        
        // When: Getting last 7 days range
        val (start, end) = DateRanges.getLast7Days()
        
        // Then: Start should be beginning of 6 days ago
        val expectedStart = sixDaysAgo.atStartOfDay(zone).toEpochSecond()
        assertEquals(expectedStart, start)
        
        // And: End should be last second of today
        val expectedEnd = today.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        assertEquals(expectedEnd, end)
    }

    @Test
    fun getLast7Days_includesExactly7Days() {
        // When: Getting last 7 days range
        val (start, end) = DateRanges.getLast7Days()
        
        // Then: The range should span exactly 7 days
        val durationSeconds = end - start + 1
        val expectedSeconds = 7L * 24 * 60 * 60 // 7 days in seconds
        assertEquals(expectedSeconds, durationSeconds)
    }

    @Test
    fun getLast7Days_startTimestampIsBeforeEndTimestamp() {
        // When: Getting last 7 days range
        val (start, end) = DateRanges.getLast7Days()
        
        // Then: Start should always be before end
        assertTrue("Start timestamp should be less than end timestamp", start < end)
    }

    @Test
    fun getThisMonth_correctlyIdentifiesFirstAndLastDay() {
        // Given: Current month's first and last day
        val today = LocalDate.now()
        val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
        val zone = ZoneId.systemDefault()
        
        // When: Getting this month's range
        val (start, end) = DateRanges.getThisMonth()
        
        // Then: Start should be beginning of first day of month
        val expectedStart = firstDayOfMonth.atStartOfDay(zone).toEpochSecond()
        assertEquals(expectedStart, start)
        
        // And: End should be last second of last day of month
        val expectedEnd = lastDayOfMonth.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        assertEquals(expectedEnd, end)
    }

    @Test
    fun getThisMonth_startTimestampIsBeforeEndTimestamp() {
        // When: Getting this month's range
        val (start, end) = DateRanges.getThisMonth()
        
        // Then: Start should always be before end
        assertTrue("Start timestamp should be less than end timestamp", start < end)
    }

    @Test
    fun getThisMonth_spansCorrectNumberOfDays() {
        // Given: Current month's length
        val today = LocalDate.now()
        val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
        val daysInMonth = lastDayOfMonth.dayOfMonth
        
        // When: Getting this month's range
        val (start, end) = DateRanges.getThisMonth()
        
        // Then: The range should span the correct number of days for this month
        val durationSeconds = end - start + 1
        val expectedSeconds = daysInMonth.toLong() * 24 * 60 * 60
        assertEquals(expectedSeconds, durationSeconds)
    }

    @Test
    fun dateRanges_areConsistentWithTimezone() {
        // Given: System default timezone
        val zone = ZoneId.systemDefault()
        
        // When: Getting various ranges
        val (todayStart, todayEnd) = DateRanges.getToday()
        val (last7Start, last7End) = DateRanges.getLast7Days()
        val (monthStart, monthEnd) = DateRanges.getThisMonth()
        
        // Then: All timestamps should be valid and in the past or present
        val now = Instant.now().epochSecond
        assertTrue("Today's end should not be in the future", todayEnd <= now + 86400) // Allow for some buffer
        assertTrue("Last 7 days end should not be in the future", last7End <= now + 86400)
        // Month end can be up to 31 days in the future (if today is the 1st of the month)
        assertTrue("This month's end should be within current month", monthEnd <= now + 32L * 86400)
        
        // And: Start times should be before their corresponding end times
        assertTrue(todayStart < todayEnd)
        assertTrue(last7Start < last7End)
        assertTrue(monthStart < monthEnd)
    }

    @Test
    fun dateRanges_handleMonthBoundaries() {
        // This test verifies the behavior works correctly regardless of when it's run
        // Even at month boundaries (last day or first day of month)
        
        // When: Getting ranges at any time
        val (todayStart, todayEnd) = DateRanges.getToday()
        val (last7Start, last7End) = DateRanges.getLast7Days()
        val (monthStart, monthEnd) = DateRanges.getThisMonth()
        
        // Then: All ranges should be valid (start < end)
        assertTrue("Today range should be valid", todayStart < todayEnd)
        assertTrue("Last 7 days range should be valid", last7Start < last7End)
        assertTrue("Month range should be valid", monthStart < monthEnd)
        
        // And: Today should be within the last 7 days
        assertTrue("Today's start should be >= last 7 days start", todayStart >= last7Start)
        assertTrue("Today's end should be <= last 7 days end", todayEnd <= last7End)
        
        // And: Today should be within this month
        assertTrue("Today's start should be >= month start", todayStart >= monthStart)
        assertTrue("Today's end should be <= month end", todayEnd <= monthEnd)
    }

    @Test
    fun dateRanges_timestampsAreInSeconds() {
        // When: Getting various ranges
        val (todayStart, todayEnd) = DateRanges.getToday()
        val (last7Start, last7End) = DateRanges.getLast7Days()
        val (monthStart, monthEnd) = DateRanges.getThisMonth()
        
        // Then: All timestamps should be reasonable epoch seconds (not milliseconds)
        // Unix epoch seconds for year 2020 onwards: > 1577836800
        // Unix epoch seconds for year 2100: < 4102444800
        assertTrue("Today start should be in valid epoch seconds range", todayStart > 1577836800L)
        assertTrue("Today start should be in valid epoch seconds range", todayStart < 4102444800L)
        assertTrue("Last 7 days start should be in valid epoch seconds range", last7Start > 1577836800L)
        assertTrue("Month start should be in valid epoch seconds range", monthStart > 1577836800L)
    }
}
