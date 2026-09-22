package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Convenience facade and backward-compatible delegate to [DeviceTimeService].
 * Fully dynamic: NO hardcoded timezone, NO fixed dates/offsets, NO mock clock.
 * Single source of truth is the user's Android device system clock and system timezone.
 */
object SmartReminderTimeHelper {

    fun deviceZoneId(): ZoneId = DeviceTimeService.currentZoneId()

    fun localTimeZone(): TimeZone = TimeZone.getTimeZone(DeviceTimeService.currentZoneId())

    fun localLocale(): Locale = Locale.getDefault()

    fun nowCalendar(epochMillis: Long = System.currentTimeMillis()): Calendar {
        val cal = Calendar.getInstance(localTimeZone(), localLocale())
        cal.timeInMillis = epochMillis
        return cal
    }

    fun currentYear(epochMillis: Long = System.currentTimeMillis()): Int = nowCalendar(epochMillis).get(Calendar.YEAR)
    fun currentMonth0(epochMillis: Long = System.currentTimeMillis()): Int = nowCalendar(epochMillis).get(Calendar.MONTH)
    fun currentDay(epochMillis: Long = System.currentTimeMillis()): Int = nowCalendar(epochMillis).get(Calendar.DAY_OF_MONTH)
    fun currentHour(epochMillis: Long = System.currentTimeMillis()): Int = nowCalendar(epochMillis).get(Calendar.HOUR_OF_DAY)
    fun currentMinute(epochMillis: Long = System.currentTimeMillis()): Int = nowCalendar(epochMillis).get(Calendar.MINUTE)

    fun isToday(year: Int, month0: Int, day: Int, epochMillis: Long = System.currentTimeMillis()): Boolean {
        val today = Instant.ofEpochMilli(epochMillis).atZone(deviceZoneId()).toLocalDate()
        return try {
            LocalDate.of(year, month0 + 1, day) == today
        } catch (_: Exception) {
            false
        }
    }

    fun isTomorrow(year: Int, month0: Int, day: Int, epochMillis: Long = System.currentTimeMillis()): Boolean {
        val tomorrow = Instant.ofEpochMilli(epochMillis).atZone(deviceZoneId()).toLocalDate().plusDays(1)
        return try {
            LocalDate.of(year, month0 + 1, day) == tomorrow
        } catch (_: Exception) {
            false
        }
    }

    fun getTodayParts(epochMillis: Long = System.currentTimeMillis()): Triple<Int, Int, Int> {
        val today = Instant.ofEpochMilli(epochMillis).atZone(deviceZoneId()).toLocalDate()
        return Triple(today.year, today.monthValue - 1, today.dayOfMonth)
    }

    fun getTomorrowParts(epochMillis: Long = System.currentTimeMillis()): Triple<Int, Int, Int> {
        val tom = Instant.ofEpochMilli(epochMillis).atZone(deviceZoneId()).toLocalDate().plusDays(1)
        return Triple(tom.year, tom.monthValue - 1, tom.dayOfMonth)
    }

    fun getIn2DaysParts(epochMillis: Long = System.currentTimeMillis()): Triple<Int, Int, Int> {
        val in2 = Instant.ofEpochMilli(epochMillis).atZone(deviceZoneId()).toLocalDate().plusDays(2)
        return Triple(in2.year, in2.monthValue - 1, in2.dayOfMonth)
    }

    fun getInitialFutureReminderTime(existingEpochMillis: Long? = null): LocalDateTimeParts {
        return DeviceTimeService.getInitialFutureReminderTime(existingEpochMillis, deviceZoneId())
    }

    fun isSelectedTimePassed(
        year: Int,
        month0: Int,
        day: Int,
        selectedHour: Int,
        selectedMinute: Int,
        epochMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return DeviceTimeService.isSelectedTimePassed(
            year = year,
            month0 = month0,
            day = day,
            selectedHour = selectedHour,
            selectedMinute = selectedMinute,
            zoneId = deviceZoneId(),
            nowInstant = Instant.ofEpochMilli(epochMillis)
        )
    }

    fun toLocalEpochMillis(
        year: Int,
        month0: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return DeviceTimeService.toLocalEpochMillis(year, month0, day, hour, minute, deviceZoneId())
    }

    fun fromEpochMillis(epochMillis: Long): LocalDateTimeParts {
        return DeviceTimeService.fromEpochMillis(epochMillis, deviceZoneId())
    }

    fun format12Hour(hour: Int, minute: Int): String {
        return DeviceTimeService.format12Hour(hour, minute)
    }

    fun formatShortDate(year: Int, month0: Int, day: Int): String {
        return DeviceTimeService.formatShortDate(year, month0, day)
    }

    fun formatFullDate(year: Int, month0: Int, day: Int): String {
        return DeviceTimeService.formatFullDate(year, month0, day)
    }

    fun getDisplayDateLabel(year: Int, month0: Int, day: Int, epochMillis: Long = System.currentTimeMillis()): String {
        return when {
            isToday(year, month0, day, epochMillis) -> "Today"
            isTomorrow(year, month0, day, epochMillis) -> "Tomorrow"
            else -> formatShortDate(year, month0, day)
        }
    }

    fun getDisplayDateLabelFromEpoch(epochMillis: Long?, currentEpochMillis: Long = System.currentTimeMillis()): String {
        if (epochMillis == null || epochMillis <= 0L) return "Today"
        val parts = fromEpochMillis(epochMillis)
        return getDisplayDateLabel(parts.year, parts.month0, parts.day, currentEpochMillis)
    }

    fun getDeviceLocalDateString(epochMillis: Long = System.currentTimeMillis()): String {
        return DeviceTimeService.getDeviceLocalDateString(epochMillis)
    }

    fun getDeviceLocalTimeString(epochMillis: Long = System.currentTimeMillis()): String {
        return DeviceTimeService.getDeviceLocalTimeString(epochMillis)
    }

    fun getDeviceTimeZoneId(): String {
        return DeviceTimeService.getDeviceTimeZoneId()
    }

    fun getDeviceTimeZoneDisplayName(): String {
        return DeviceTimeService.getDeviceTimeZoneDisplayName()
    }

    fun getDeviceUtcOffsetString(epochMillis: Long = System.currentTimeMillis()): String {
        return DeviceTimeService.getDeviceUtcOffsetString(epochMillis)
    }

    fun getDeviceTimeZoneString(epochMillis: Long = System.currentTimeMillis()): String {
        return DeviceTimeService.getDeviceTimeZoneString(epochMillis)
    }

    fun calculateDaysUntil(examDateString: String?): Int? {
        return DeviceTimeService.calculateDaysUntil(examDateString)
    }
}
