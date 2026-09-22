package com.example

import com.example.util.DeviceTimeService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class DeviceTimeServiceTest {

    @Test
    fun testSystemDefaultTimeZoneMatches() {
        val systemZone = ZoneId.systemDefault()
        val serviceZone = DeviceTimeService.currentZoneId()
        assertEquals(systemZone.id, serviceZone.id)
    }

    @Test
    fun testTodayAndTomorrowParts() {
        val today = DeviceTimeService.todayLocalDate()
        val (y, m, d) = DeviceTimeService.getTodayParts()
        assertEquals(today.year, y)
        assertEquals(today.monthValue - 1, m)
        assertEquals(today.dayOfMonth, d)

        val tomorrow = today.plusDays(1)
        val (ty, tm, td) = DeviceTimeService.getTomorrowParts()
        assertEquals(tomorrow.year, ty)
        assertEquals(tomorrow.monthValue - 1, tm)
        assertEquals(tomorrow.dayOfMonth, td)
    }

    @Test
    fun testIsTodayAndIsTomorrow() {
        val (y, m, d) = DeviceTimeService.getTodayParts()
        assertTrue(DeviceTimeService.isToday(y, m, d))
        assertFalse(DeviceTimeService.isTomorrow(y, m, d))

        val (ty, tm, td) = DeviceTimeService.getTomorrowParts()
        assertTrue(DeviceTimeService.isTomorrow(ty, tm, td))
        assertFalse(DeviceTimeService.isToday(ty, tm, td))
    }

    @Test
    fun testEpochConversionRoundTrip() {
        val (y, m, d) = DeviceTimeService.getTodayParts()
        val hour = 14
        val minute = 25

        val epochMillis = DeviceTimeService.toLocalEpochMillis(y, m, d, hour, minute)
        val parts = DeviceTimeService.fromEpochMillis(epochMillis)

        assertEquals(y, parts.year)
        assertEquals(m, parts.month0)
        assertEquals(d, parts.day)
        assertEquals(hour, parts.hour)
        assertEquals(minute, parts.minute)
    }

    @Test
    fun testTomorrowIsNeverPassed() {
        val (ty, tm, td) = DeviceTimeService.getTomorrowParts()
        assertFalse(DeviceTimeService.isSelectedTimePassed(ty, tm, td, 0, 0))
        assertFalse(DeviceTimeService.isSelectedTimePassed(ty, tm, td, 12, 0))
        assertFalse(DeviceTimeService.isSelectedTimePassed(ty, tm, td, 23, 59))
    }

    @Test
    fun testTodayPastTimeIsIdentified() {
        val now = DeviceTimeService.nowZoned()
        val currentHour = now.hour
        val currentMinute = now.minute

        val (y, m, d) = DeviceTimeService.getTodayParts()

        if (currentHour > 0 || currentMinute > 2) {
            val pastTime = now.minusMinutes(5)
            val isPassed = DeviceTimeService.isSelectedTimePassed(
                y, m, d, pastTime.hour, pastTime.minute
            )
            assertTrue("5 minutes ago today must be flagged as passed", isPassed)
        }
    }

    @Test
    fun testTodayFutureTimeIsNotPassed() {
        val now = DeviceTimeService.nowZoned()
        val currentHour = now.hour

        val (y, m, d) = DeviceTimeService.getTodayParts()

        if (currentHour < 23) {
            val futureTime = now.plusHours(1)
            if (futureTime.dayOfMonth == d) {
                val isPassed = DeviceTimeService.isSelectedTimePassed(
                    y, m, d, futureTime.hour, futureTime.minute
                )
                assertFalse("1 hour in future today must not be flagged as passed", isPassed)
            }
        }
    }

    @Test
    fun testDiagnosticStrings() {
        val dateStr = DeviceTimeService.getDeviceLocalDateString()
        val timeStr = DeviceTimeService.getDeviceLocalTimeString()
        val tzId = DeviceTimeService.getDeviceTimeZoneId()
        val offsetStr = DeviceTimeService.getDeviceUtcOffsetString()

        assertTrue(dateStr.isNotBlank())
        assertTrue(timeStr.isNotBlank())
        assertTrue(tzId.isNotBlank())
        assertTrue(offsetStr.isNotBlank())
    }

    @Test
    fun testCalculateDaysUntil() {
        val today = DeviceTimeService.todayLocalDate()
        val days5Ahead = today.plusDays(5)
        val formatted = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.US).format(days5Ahead)
        val daysUntil = DeviceTimeService.calculateDaysUntil(formatted)
        assertNotNull(daysUntil)
        assertEquals(5, daysUntil)
    }

    @Test
    fun testMultiTimezoneBehavior() {
        val originalTz = TimeZone.getDefault()
        try {
            val timezones = listOf("Asia/Kolkata", "America/New_York", "Europe/London", "Asia/Tokyo", "Australia/Sydney")
            for (tzName in timezones) {
                TimeZone.setDefault(TimeZone.getTimeZone(tzName))
                val nowZoned = DeviceTimeService.nowZoned()
                assertEquals(tzName, nowZoned.zone.id)

                val (y, m, d) = DeviceTimeService.getTodayParts()
                assertTrue(DeviceTimeService.isToday(y, m, d))

                val (ty, tm, td) = DeviceTimeService.getTomorrowParts()
                assertTrue(DeviceTimeService.isTomorrow(ty, tm, td))
                assertFalse(DeviceTimeService.isSelectedTimePassed(ty, tm, td, 9, 0))
            }
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
