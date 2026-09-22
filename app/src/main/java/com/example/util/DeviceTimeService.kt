package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Centralized, authoritative device date, time, and timezone service.
 * - Single source of truth is the user's actual Android DEVICE settings.
 * - Device timezone → Device local date/time → App date/time → Reminder scheduling.
 * - Uses ZoneId.systemDefault() and java.time.* APIs dynamically.
 * - ZERO hardcoded timezones (works universally in India, USA, UK, UAE, Japan, Australia, etc.).
 * - Automatically adapts when the user travels or changes the device timezone.
 * - Never adds or subtracts fixed offsets or arbitrary hours/minutes.
 * - "Today" is strictly the current calendar date in the device's local timezone.
 * - "Tomorrow" is strictly one calendar day after Today in the device's local timezone.
 * - Tomorrow and future dates are NEVER treated as passed.
 */
/**
 * Data structure holding decomposed local date and time components.
 * Note: month0 is 0-indexed (0 = January, 11 = December) for direct compatibility with Android UI pickers.
 */
data class LocalDateTimeParts(
    val year: Int,
    val month0: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
)

object DeviceTimeService {

    /**
     * Resolves the current system default ZoneId directly from the Android device.
     */
    fun currentZoneId(): ZoneId {
        return try {
            ZoneId.systemDefault()
        } catch (_: Throwable) {
            ZoneId.of(TimeZone.getDefault().id)
        }
    }

    /**
     * Returns the current ZonedDateTime using the device's dynamic system default ZoneId.
     */
    fun nowZoned(): ZonedDateTime = ZonedDateTime.now(currentZoneId())

    /**
     * Returns the current LocalDate in the device's local timezone.
     */
    fun currentLocalDate(): LocalDate = LocalDate.now(currentZoneId())

    /**
     * Returns the current LocalTime in the device's local timezone.
     */
    fun currentLocalTime(): LocalTime = LocalTime.now(currentZoneId())

    /**
     * Returns the current Instant from the device system clock.
     */
    fun currentInstant(): Instant = Instant.now()

    /**
     * Returns current epoch milliseconds from the device system clock.
     */
    fun currentEpochMillis(): Long = System.currentTimeMillis()

    // --- Dynamic Date Calculations ---

    fun todayLocalDate(): LocalDate = currentLocalDate()

    fun tomorrowLocalDate(): LocalDate = todayLocalDate().plusDays(1)

    fun in2DaysLocalDate(): LocalDate = todayLocalDate().plusDays(2)

    fun isToday(date: LocalDate): Boolean = date == todayLocalDate()

    fun isTomorrow(date: LocalDate): Boolean = date == tomorrowLocalDate()

    fun isToday(year: Int, month0: Int, day: Int): Boolean {
        return try {
            LocalDate.of(year, month0 + 1, day) == todayLocalDate()
        } catch (_: Exception) {
            false
        }
    }

    fun isTomorrow(year: Int, month0: Int, day: Int): Boolean {
        return try {
            LocalDate.of(year, month0 + 1, day) == tomorrowLocalDate()
        } catch (_: Exception) {
            false
        }
    }

    fun isTodayEpoch(epochMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(epochMillis).atZone(currentZoneId()).toLocalDate()
        return date == todayLocalDate()
    }

    fun isTomorrowEpoch(epochMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(epochMillis).atZone(currentZoneId()).toLocalDate()
        return date == tomorrowLocalDate()
    }

    fun getTodayParts(): Triple<Int, Int, Int> {
        val today = todayLocalDate()
        return Triple(today.year, today.monthValue - 1, today.dayOfMonth)
    }

    fun getTomorrowParts(): Triple<Int, Int, Int> {
        val tom = tomorrowLocalDate()
        return Triple(tom.year, tom.monthValue - 1, tom.dayOfMonth)
    }

    fun getIn2DaysParts(): Triple<Int, Int, Int> {
        val in2 = in2DaysLocalDate()
        return Triple(in2.year, in2.monthValue - 1, in2.dayOfMonth)
    }

    // --- Instant & Epoch Conversions ---

    /**
     * Converts a local date/time into an absolute Instant based on the device's ZoneId.
     */
    fun toInstant(
        year: Int,
        month0: Int,
        day: Int,
        hour: Int,
        minute: Int,
        zoneId: ZoneId = currentZoneId()
    ): Instant {
        val localDate = LocalDate.of(year, month0 + 1, day)
        val localTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59), 0, 0)
        return ZonedDateTime.of(localDate, localTime, zoneId).toInstant()
    }

    /**
     * Converts a local date/time into epoch milliseconds based on the device's ZoneId.
     */
    fun toLocalEpochMillis(
        year: Int,
        month0: Int,
        day: Int,
        hour: Int,
        minute: Int,
        zoneId: ZoneId = currentZoneId()
    ): Long {
        return toInstant(year, month0, day, hour, minute, zoneId).toEpochMilli()
    }

    /**
     * Deconstructs epoch milliseconds into local date/time parts using the device's ZoneId.
     */
    fun fromEpochMillis(epochMillis: Long, zoneId: ZoneId = currentZoneId()): LocalDateTimeParts {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return LocalDateTimeParts(
            year = zdt.year,
            month0 = zdt.monthValue - 1,
            day = zdt.dayOfMonth,
            hour = zdt.hour,
            minute = zdt.minute
        )
    }

    // --- Time Passed & Validity Logic ---

    /**
     * Determines whether the chosen reminder date & time has already passed according to the device clock.
     * - Tomorrow and any future date: NEVER passed (always returns false).
     * - Past date: returns true.
     * - Today: returns true only if the selected time is before or equal to current device time.
     */
    fun isSelectedTimePassed(
        year: Int,
        month0: Int,
        day: Int,
        selectedHour: Int,
        selectedMinute: Int,
        zoneId: ZoneId = currentZoneId(),
        nowInstant: Instant = Instant.now()
    ): Boolean {
        val today = nowInstant.atZone(zoneId).toLocalDate()
        val selectedDate = try {
            LocalDate.of(year, month0 + 1, day)
        } catch (_: Exception) {
            today
        }

        if (selectedDate.isBefore(today)) return true
        if (selectedDate.isAfter(today)) return false // Tomorrow and any future date is NEVER passed

        // Same day (Today): compare times against actual current local time
        val nowTime = nowInstant.atZone(zoneId).toLocalTime()
        val selTime = try {
            LocalTime.of(selectedHour.coerceIn(0, 23), selectedMinute.coerceIn(0, 59))
        } catch (_: Exception) {
            nowTime
        }

        return selTime.isBefore(nowTime) || (selTime.hour == nowTime.hour && selTime.minute <= nowTime.minute)
    }

    /**
     * Generates a suggested future reminder time (30 minutes from current device local time).
     * If adding 30 minutes crosses midnight, the date cleanly advances to tomorrow.
     */
    fun getInitialFutureReminderTime(
        existingEpochMillis: Long? = null,
        zoneId: ZoneId = currentZoneId()
    ): LocalDateTimeParts {
        val now = ZonedDateTime.now(zoneId)
        val target = if (existingEpochMillis != null && existingEpochMillis > System.currentTimeMillis()) {
            Instant.ofEpochMilli(existingEpochMillis).atZone(zoneId)
        } else {
            now.plusMinutes(30).truncatedTo(ChronoUnit.MINUTES)
        }
        return LocalDateTimeParts(
            year = target.year,
            month0 = target.monthValue - 1,
            day = target.dayOfMonth,
            hour = target.hour,
            minute = target.minute
        )
    }

    // --- Formatters ---

    fun format12Hour(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        return time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }

    fun formatShortDate(year: Int, month0: Int, day: Int): String {
        val date = LocalDate.of(year, month0 + 1, day)
        return date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }

    fun formatFullDate(year: Int, month0: Int, day: Int): String {
        val date = LocalDate.of(year, month0 + 1, day)
        return date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()))
    }

    fun getDisplayDateLabel(year: Int, month0: Int, day: Int): String {
        return when {
            isToday(year, month0, day) -> "Today"
            isTomorrow(year, month0, day) -> "Tomorrow"
            else -> formatShortDate(year, month0, day)
        }
    }

    fun getDisplayDateLabelFromEpoch(epochMillis: Long?, zoneId: ZoneId = currentZoneId()): String {
        if (epochMillis == null || epochMillis <= 0L) return "Today"
        val parts = fromEpochMillis(epochMillis, zoneId)
        return getDisplayDateLabel(parts.year, parts.month0, parts.day)
    }

    // --- Device System Clock Diagnostics ---

    fun getDeviceLocalDateString(epochMillis: Long = System.currentTimeMillis()): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(currentZoneId())
        return zdt.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault()))
    }

    fun getDeviceLocalTimeString(epochMillis: Long = System.currentTimeMillis()): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(currentZoneId())
        return zdt.format(DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault()))
    }

    fun getDeviceTimeZoneId(): String {
        return currentZoneId().id
    }

    fun getDeviceTimeZoneDisplayName(): String {
        val tz = TimeZone.getTimeZone(currentZoneId())
        return tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT, Locale.getDefault())
    }

    fun getDeviceUtcOffsetString(epochMillis: Long = System.currentTimeMillis()): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(currentZoneId())
        val totalSeconds = zdt.offset.totalSeconds
        val sign = if (totalSeconds >= 0) "+" else "-"
        val absSeconds = Math.abs(totalSeconds)
        val hours = absSeconds / 3600
        val mins = (absSeconds % 3600) / 60
        return String.format(Locale.US, "UTC%s%02d:%02d", sign, hours, mins)
    }

    fun getDeviceTimeZoneString(epochMillis: Long = System.currentTimeMillis()): String {
        val id = getDeviceTimeZoneId()
        val displayName = getDeviceTimeZoneDisplayName()
        val offset = getDeviceUtcOffsetString(epochMillis)
        return if (displayName.isNotBlank() && displayName != id) {
            "$id ($displayName, $offset)"
        } else {
            "$id ($offset)"
        }
    }

    /**
     * Dynamically calculates days until an exam from the current device local date.
     */
    fun calculateDaysUntil(examDateString: String?): Int? {
        if (examDateString.isNullOrBlank()) return null
        return try {
            val parsedDate = try {
                val caseInsensitiveFormatter = java.time.format.DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMM d, yyyy")
                    .toFormatter(Locale.US)
                LocalDate.parse(examDateString.trim(), caseInsensitiveFormatter)
            } catch (_: Exception) {
                try {
                    LocalDate.parse(examDateString.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) {
                    null
                }
            }
            if (parsedDate != null) {
                val days = ChronoUnit.DAYS.between(currentLocalDate(), parsedDate).toInt()
                Math.max(0, days)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Authoritative task due date parser that resolves any date string or relative term
     * into a concrete LocalDate in the device's local timezone.
     */
    fun parseTaskDueDateToLocalDate(dueDateString: String?): LocalDate? {
        if (dueDateString.isNullOrBlank()) return null
        val trimmed = dueDateString.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Relative keyword matching
        if (lower == "today" || lower == "tonight") {
            return todayLocalDate()
        }
        if (lower == "yesterday") {
            return todayLocalDate().minusDays(1)
        }
        if (lower == "tomorrow") {
            return tomorrowLocalDate()
        }
        if (lower == "day after tomorrow" || lower == "in 2 days") {
            return todayLocalDate().plusDays(2)
        }
        if (lower == "in 3 days") {
            return todayLocalDate().plusDays(3)
        }
        if (lower == "last week" || lower == "past") {
            return todayLocalDate().minusDays(7)
        }

        // Relative days ago: e.g. "2 days ago", "3 days ago"
        val agoMatch = Regex("""(\d+)\s+days?\s+ago""").find(lower)
        if (agoMatch != null) {
            val days = agoMatch.groupValues[1].toLongOrNull() ?: 1L
            return todayLocalDate().minusDays(days)
        }

        // Relative in days: e.g. "in 4 days"
        val inDaysMatch = Regex("""in\s+(\d+)\s+days?""").find(lower)
        if (inDaysMatch != null) {
            val days = inDaysMatch.groupValues[1].toLongOrNull() ?: 1L
            return todayLocalDate().plusDays(days)
        }

        val currentYear = todayLocalDate().year

        // 2. Formatted date patterns with year
        val patternsWithYear = listOf(
            "MMM d, yyyy",
            "MMMM d, yyyy",
            "EEEE, MMM d, yyyy",
            "EEE, MMM d, yyyy",
            "EEEE, MMMM d, yyyy",
            "yyyy-MM-dd",
            "M/d/yyyy",
            "MM/dd/yyyy",
            "d/M/yyyy",
            "dd/MM/yyyy"
        )
        for (pat in patternsWithYear) {
            try {
                val formatter = java.time.format.DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pat)
                    .toFormatter(Locale.US)
                return LocalDate.parse(trimmed, formatter)
            } catch (_: Exception) {}
        }

        // 3. Formatted date patterns without year (defaults to current device year)
        val patternsWithoutYear = listOf(
            "EEEE, MMM d",
            "EEE, MMM d",
            "EEEE, MMMM d",
            "EEE, MMMM d",
            "MMM d",
            "MMMM d",
            "M/d",
            "MM/dd",
            "d/M",
            "dd/MM"
        )
        for (pat in patternsWithoutYear) {
            try {
                val formatter = java.time.format.DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pat)
                    .parseDefaulting(java.time.temporal.ChronoField.YEAR, currentYear.toLong())
                    .toFormatter(Locale.US)
                return LocalDate.parse(trimmed, formatter)
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Determines whether the given date string corresponds to a date strictly in the past
     * relative to the device's current calendar date.
     */
    fun isDateBeforeToday(dateString: String?): Boolean {
        if (dateString.isNullOrBlank()) return false
        val lower = dateString.trim().lowercase(Locale.ROOT)
        if (lower == "yesterday" || lower == "last week" || lower == "past" || lower.contains("ago")) {
            return true
        }
        if (lower == "today" || lower == "tonight" || lower == "tomorrow" || lower.contains("next") || lower == "this week") {
            return false
        }
        val localDate = parseTaskDueDateToLocalDate(dateString) ?: return false
        return localDate.isBefore(todayLocalDate())
    }

    /**
     * Determines whether the given date string corresponds to today in the device local timezone.
     */
    fun isDateToday(dateString: String?): Boolean {
        if (dateString.isNullOrBlank()) return false
        val lower = dateString.trim().lowercase(Locale.ROOT)
        if (lower == "today" || lower == "tonight") return true
        val localDate = parseTaskDueDateToLocalDate(dateString) ?: return false
        return localDate == todayLocalDate()
    }

    /**
     * Returns signed calendar days from today to the date string (<0 if overdue/past, 0 if today, >0 if future).
     */
    fun calculateDaysFromToday(dateString: String?): Int? {
        val targetDate = parseTaskDueDateToLocalDate(dateString) ?: return null
        return ChronoUnit.DAYS.between(todayLocalDate(), targetDate).toInt()
    }

    /**
     * Normalizes day abbreviations or variations to standard full day names ("Monday", "Tuesday", etc.).
     */
    fun normalizeDay(day: String): String {
        return when (day.trim().lowercase(Locale.ROOT)) {
            "mon", "monday" -> "Monday"
            "tue", "tuesday" -> "Tuesday"
            "wed", "wednesday" -> "Wednesday"
            "thu", "thursday" -> "Thursday"
            "fri", "friday" -> "Friday"
            "sat", "saturday" -> "Saturday"
            "sun", "sunday" -> "Sunday"
            else -> day.trim()
        }
    }

    /**
     * Returns the full day of week name for the device's current local date in user's timezone.
     */
    fun currentDayOfWeekName(): String {
        return when (currentLocalDate().dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "Monday"
            java.time.DayOfWeek.TUESDAY -> "Tuesday"
            java.time.DayOfWeek.WEDNESDAY -> "Wednesday"
            java.time.DayOfWeek.THURSDAY -> "Thursday"
            java.time.DayOfWeek.FRIDAY -> "Friday"
            java.time.DayOfWeek.SATURDAY -> "Saturday"
            java.time.DayOfWeek.SUNDAY -> "Sunday"
        }
    }

    /**
     * Parses a time string (e.g. "09:00 AM", "9:00 am", "09:00", "14:30") into minutes from midnight.
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        val cleaned = timeStr.trim().uppercase(Locale.ROOT)
        try {
            val f = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            return LocalTime.parse(cleaned, f).let { it.hour * 60 + it.minute }
        } catch (_: Exception) {}
        try {
            val f = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            return LocalTime.parse(cleaned, f).let { it.hour * 60 + it.minute }
        } catch (_: Exception) {}
        try {
            val f = DateTimeFormatter.ofPattern("H:mm", Locale.US)
            return LocalTime.parse(cleaned, f).let { it.hour * 60 + it.minute }
        } catch (_: Exception) {}
        try {
            val f = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
            return LocalTime.parse(cleaned, f).let { it.hour * 60 + it.minute }
        } catch (_: Exception) {}
        return 0
    }
}

fun parseTimeStringToMinutes(timeStr: String): Int = DeviceTimeService.parseTimeToMinutes(timeStr)
