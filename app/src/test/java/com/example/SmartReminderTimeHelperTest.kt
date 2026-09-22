package com.example

import com.example.util.SmartReminderTimeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

class SmartReminderTimeHelperTest {

    @Test
    fun testTodayAndTomorrowCalculations() {
        val (todayY, todayM, todayD) = SmartReminderTimeHelper.getTodayParts()
        val (tomY, tomM, tomD) = SmartReminderTimeHelper.getTomorrowParts()

        assertTrue("Today should be identified as today", SmartReminderTimeHelper.isToday(todayY, todayM, todayD))
        assertFalse("Today should not be tomorrow", SmartReminderTimeHelper.isTomorrow(todayY, todayM, todayD))

        assertTrue("Tomorrow should be identified as tomorrow", SmartReminderTimeHelper.isTomorrow(tomY, tomM, tomD))
        assertFalse("Tomorrow should not be today", SmartReminderTimeHelper.isToday(tomY, tomM, tomD))
    }

    @Test
    fun testTomorrowNeverFlagsAsPassed() {
        val (tomY, tomM, tomD) = SmartReminderTimeHelper.getTomorrowParts()
        // Early morning tomorrow (00:00 or 06:00) should NEVER be flagged as passed
        assertFalse(
            "Tomorrow morning should not be passed",
            SmartReminderTimeHelper.isSelectedTimePassed(tomY, tomM, tomD, 0, 0)
        )
        assertFalse(
            "Tomorrow afternoon should not be passed",
            SmartReminderTimeHelper.isSelectedTimePassed(tomY, tomM, tomD, 14, 30)
        )
        assertFalse(
            "Tomorrow night should not be passed",
            SmartReminderTimeHelper.isSelectedTimePassed(tomY, tomM, tomD, 23, 59)
        )
    }

    @Test
    fun testLocalEpochPreservesDateAndTimeComponents() {
        val (todayY, todayM, todayD) = SmartReminderTimeHelper.getTodayParts()
        val testHour = 15
        val testMinute = 45

        val epochMillis = SmartReminderTimeHelper.toLocalEpochMillis(
            todayY, todayM, todayD, testHour, testMinute
        )

        val parts = SmartReminderTimeHelper.fromEpochMillis(epochMillis)
        assertEquals("Year must match", todayY, parts.year)
        assertEquals("Month must match", todayM, parts.month0)
        assertEquals("Day must match", todayD, parts.day)
        assertEquals("Hour must match", testHour, parts.hour)
        assertEquals("Minute must match", testMinute, parts.minute)
    }

    @Test
    fun testTimePassedLogicForToday() {
        val localCal = SmartReminderTimeHelper.nowCalendar()
        val (todayY, todayM, todayD) = SmartReminderTimeHelper.getTodayParts()

        val currentHour = localCal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = localCal.get(Calendar.MINUTE)

        // If not at very end of day, next hour should NOT be passed
        if (currentHour < 23) {
            val futureHour = currentHour + 1
            assertFalse(
                "Future hour today should not be flagged as passed",
                SmartReminderTimeHelper.isSelectedTimePassed(todayY, todayM, todayD, futureHour, 0)
            )
        }

        // If not at very start of day, earlier hour SHOULD be passed
        if (currentHour > 0) {
            val pastHour = currentHour - 1
            assertTrue(
                "Past hour today must be flagged as passed",
                SmartReminderTimeHelper.isSelectedTimePassed(todayY, todayM, todayD, pastHour, 0)
            )
        }
    }

    @Test
    fun testDeviceTimeZoneResolutionIsDynamic() {
        val tz = SmartReminderTimeHelper.localTimeZone()
        val defaultTz = TimeZone.getDefault()
        assertEquals("Timezone must dynamically match device default", defaultTz.id, tz.id)

        val zoneId = SmartReminderTimeHelper.deviceZoneId()
        val expectedZoneId = ZoneId.systemDefault()
        assertEquals("ZoneId must match systemDefault", expectedZoneId.id, zoneId.id)

        val tzStr = SmartReminderTimeHelper.getDeviceTimeZoneString()
        assertTrue("Timezone display must contain timezone id or offset", tzStr.contains(defaultTz.id) || tzStr.contains("UTC"))
    }

    @Test
    fun testExact12HourFormats() {
        assertEquals("12:05 AM", SmartReminderTimeHelper.format12Hour(0, 5))
        assertEquals("7:30 AM", SmartReminderTimeHelper.format12Hour(7, 30))
        assertEquals("11:18 AM", SmartReminderTimeHelper.format12Hour(11, 18))
        assertEquals("1:06 PM", SmartReminderTimeHelper.format12Hour(13, 6))
        assertEquals("4:45 PM", SmartReminderTimeHelper.format12Hour(16, 45))
        assertEquals("11:59 PM", SmartReminderTimeHelper.format12Hour(23, 59))
    }

    @Test
    fun testDisplayDateLabelTodayAndTomorrow() {
        val (todayY, todayM, todayD) = SmartReminderTimeHelper.getTodayParts()
        val (tomY, tomM, tomD) = SmartReminderTimeHelper.getTomorrowParts()

        assertEquals("Today", SmartReminderTimeHelper.getDisplayDateLabel(todayY, todayM, todayD))
        assertEquals("Tomorrow", SmartReminderTimeHelper.getDisplayDateLabel(tomY, tomM, tomD))
    }

    @Test
    fun testInitialFutureReminderTimeIsAlwaysInFuture() {
        val initialParts = SmartReminderTimeHelper.getInitialFutureReminderTime()
        val epoch = SmartReminderTimeHelper.toLocalEpochMillis(
            initialParts.year,
            initialParts.month0,
            initialParts.day,
            initialParts.hour,
            initialParts.minute
        )
        assertTrue(
            "Initial reminder suggestion must be strictly in the future",
            epoch > System.currentTimeMillis()
        )
    }

    @Test
    fun testTodayFutureTimeIsNotPassed() {
        val localCal = Calendar.getInstance(TimeZone.getDefault())
        localCal.add(Calendar.MINUTE, 5)
        val futureParts = SmartReminderTimeHelper.fromEpochMillis(localCal.timeInMillis)

        val (todayY, todayM, todayD) = SmartReminderTimeHelper.getTodayParts()
        if (futureParts.year == todayY && futureParts.month0 == todayM && futureParts.day == todayD) {
            val passed = SmartReminderTimeHelper.isSelectedTimePassed(
                todayY, todayM, todayD, futureParts.hour, futureParts.minute
            )
            assertFalse("Reminder 5 minutes in future today must NOT be flagged as passed", passed)
        }
    }

    @Test
    fun testTomorrowAnyTimeIsNeverPassed() {
        val (tomY, tomM, tomD) = SmartReminderTimeHelper.getTomorrowParts()
        assertFalse("Tomorrow at 10:00 AM must NEVER be flagged as passed",
            SmartReminderTimeHelper.isSelectedTimePassed(tomY, tomM, tomD, 10, 0))
        assertFalse("Tomorrow at 11:59 PM must NEVER be flagged as passed",
            SmartReminderTimeHelper.isSelectedTimePassed(tomY, tomM, tomD, 23, 59))
    }

    @Test
    fun testTodayGenuinelyPastTimeIsFlaggedAsPassed() {
        val localCal = Calendar.getInstance(TimeZone.getDefault())
        val currentHour = localCal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = localCal.get(Calendar.MINUTE)
        val (todayY, todayM, todayD) = SmartReminderTimeHelper.getTodayParts()

        if (currentHour > 0 || currentMinute > 2) {
            localCal.add(Calendar.MINUTE, -3)
            val pastHour = localCal.get(Calendar.HOUR_OF_DAY)
            val pastMin = localCal.get(Calendar.MINUTE)

            val passed = SmartReminderTimeHelper.isSelectedTimePassed(
                todayY, todayM, todayD, pastHour, pastMin
            )
            assertTrue("Today genuinely past time must be flagged as passed", passed)
        }
    }

    @Test
    fun testDiagnosticStringsArePopulated() {
        val dateStr = SmartReminderTimeHelper.getDeviceLocalDateString()
        val timeStr = SmartReminderTimeHelper.getDeviceLocalTimeString()
        val tzStr = SmartReminderTimeHelper.getDeviceTimeZoneString()
        val offsetStr = SmartReminderTimeHelper.getDeviceUtcOffsetString()

        assertTrue("Date string should not be blank", dateStr.isNotBlank())
        assertTrue("Time string should not be blank", timeStr.isNotBlank())
        assertTrue("Timezone string should not be blank", tzStr.isNotBlank())
        assertTrue("Offset string should not be blank", offsetStr.isNotBlank())
        assertTrue("Offset string should start with UTC", offsetStr.startsWith("UTC"))
    }
}
