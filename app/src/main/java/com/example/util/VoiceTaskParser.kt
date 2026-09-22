package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Structured parsed result from a spoken voice homework sentence.
 */
data class ParsedVoiceTask(
    val originalSpokenText: String,
    val title: String,
    val subject: String,
    val dueDate: String,
    val dateDisplay: String,
    val timeDisplay: String,
    val year: Int,
    val month0: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int,
    val priority: String, // "High", "Medium", "Low"
    val xpReward: Int,
    val reminderEnabled: Boolean = true,
    val reminderEpochMillis: Long? = null
)

object VoiceTaskParser {

    private val SUBJECT_KEYWORDS = mapOf(
        "Physics" to listOf(
            "physics", "kinematics", "optics", "thermodynamics", "electromagnetism", "mechanics",
            "bhautiki", "bhautik", "भौतिकी", "भौतिक", "फिजिक्स"
        ),
        "Chemistry" to listOf(
            "chemistry", "stoichiometry", "organic chemistry", "inorganic", "periodic table", "chemical",
            "rasayan", "rasayanik", "रसायन", "रसायनिक", "केमिस्ट्री"
        ),
        "Mathematics" to listOf(
            "maths", "mathematics", "math", "calculus", "algebra", "geometry", "trigonometry",
            "statistics", "integration", "derivatives", "ganit", "hisab", "गणित", "मैथ्स", "मैथ"
        ),
        "Biology" to listOf(
            "biology", "bio", "cell division", "respiration", "genetics", "botany", "zoology", "anatomy",
            "jeev vigyan", "जीव विज्ञान", "जीवविज्ञान", "बायोलॉजी"
        ),
        "Computer Science" to listOf(
            "computer science", "cs", "coding", "python", "java", "quicksort", "algorithms",
            "data structures", "programming", "computer", "कम्प्यूटर", "कंप्यूटर"
        ),
        "English" to listOf(
            "english", "literature", "grammar", "essay", "shakespeare", "poetry", "novel",
            "angrezi", "अंग्रेजी", "इंग्लिश"
        ),
        "History" to listOf(
            "history", "revolution", "world war", "civilization", "geography", "social science",
            "itihaas", "इतिहास", "हिस्ट्री", "भूगोल"
        )
    )

    /**
     * Parses natural language spoken sentence into a structured study task.
     * Example: "Tomorrow at 7 AM revise Physics for 30 minutes"
     */
    fun parse(spokenText: String, baseZoneId: ZoneId = DeviceTimeService.currentZoneId()): ParsedVoiceTask {
        val cleanInput = normalizeIndicNumerals(spokenText.trim().replace(Regex("[.,!?;]+$"), ""))
        val lower = cleanInput.lowercase(Locale.ROOT)

        // 1. Extract Duration
        val durationMinutes = extractDuration(lower)

        // 2. Extract Time
        val (parsedHour, parsedMinute, timeMatchedStr) = extractTime(lower)

        // 3. Extract Date & Day
        val (targetDate, dueDateStr, dateMatchedStr) = extractDate(lower, parsedHour, parsedMinute, baseZoneId)

        // 4. Extract Subject
        val detectedSubject = extractSubject(lower)

        // 5. Extract Priority
        val (priority, priorityMatchedStr) = extractPriority(lower)

        // 6. Extract Task Title by removing meta tokens
        val extractedTitle = extractCleanTitle(
            original = cleanInput,
            tokensToRemove = listOfNotNull(
                timeMatchedStr,
                dateMatchedStr,
                priorityMatchedStr,
                findDurationStr(lower)
            ),
            subject = detectedSubject
        )

        // 7. Calculate XP Reward
        val xpReward = calculateXp(durationMinutes, priority)

        // 8. Compute Reminder Epoch Millis
        val targetZdt = ZonedDateTime.of(
            targetDate,
            LocalTime.of(parsedHour, parsedMinute),
            baseZoneId
        )
        val epochMillis = targetZdt.toInstant().toEpochMilli()

        val timeDisplay = DeviceTimeService.format12Hour(parsedHour, parsedMinute)
        val dateDisplay = when {
            DeviceTimeService.isToday(targetDate.year, targetDate.monthValue - 1, targetDate.dayOfMonth) -> "Today, " + targetDate.format(DateTimeFormatter.ofPattern("MMM d"))
            DeviceTimeService.isTomorrow(targetDate.year, targetDate.monthValue - 1, targetDate.dayOfMonth) -> "Tomorrow, " + targetDate.format(DateTimeFormatter.ofPattern("MMM d"))
            else -> targetDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        }

        return ParsedVoiceTask(
            originalSpokenText = cleanInput,
            title = extractedTitle,
            subject = detectedSubject,
            dueDate = dueDateStr,
            dateDisplay = dateDisplay,
            timeDisplay = timeDisplay,
            year = targetDate.year,
            month0 = targetDate.monthValue - 1,
            day = targetDate.dayOfMonth,
            hour = parsedHour,
            minute = parsedMinute,
            durationMinutes = durationMinutes,
            priority = priority,
            xpReward = xpReward,
            reminderEnabled = true,
            reminderEpochMillis = epochMillis
        )
    }

    private fun normalizeIndicNumerals(input: String): String {
        val indic = "०१२३४५६७८९"
        var out = input
        for (i in indic.indices) {
            out = out.replace(indic[i], ('0' + i))
        }
        return out
    }

    private fun extractDuration(text: String): Int {
        // e.g. "for 1.5 hours", "for 2 hours", "1 hour", "2 ghante"
        val hourRegex = Regex("""(?:for\s+)?(\d+(?:\.\d+)?)\s*(?:hours?|hrs?|hr|ghante?|ghanto|घंटे|घंटा)""")
        hourRegex.find(text)?.let {
            val hours = it.groupValues[1].toDoubleOrNull() ?: 1.0
            return (hours * 60).toInt().coerceIn(10, 480)
        }

        // e.g. "for 30 minutes", "45 mins", "15 min", "30 minat", "30 मिनट"
        val minRegex = Regex("""(?:for\s+)?(\d+)\s*(?:minutes?|mins?|min|minat|minut|मिनट)""")
        minRegex.find(text)?.let {
            val mins = it.groupValues[1].toIntOrNull() ?: 30
            return mins.coerceIn(5, 480)
        }

        if (text.contains("half an hour") || text.contains("half hour") ||
            text.contains("aadha ghanta") || text.contains("aadhe ghante") || text.contains("आधा घंटा")
        ) return 30

        if (text.contains("an hour") || text.contains("one hour") ||
            text.contains("ek ghanta") || text.contains("एक घंटा")
        ) return 60

        return 30 // Default 30 mins
    }

    private fun findDurationStr(text: String): String? {
        val patterns = listOf(
            Regex("""(?:for\s+)?\d+(?:\.\d+)?\s*(?:hours?|hrs?|hr|ghante?|ghanto|घंटे|घंटा)"""),
            Regex("""(?:for\s+)?\d+\s*(?:minutes?|mins?|min|minat|minut|मिनट)"""),
            Regex("""half an hour"""),
            Regex("""half hour"""),
            Regex("""aadha ghanta"""),
            Regex("""aadhe ghante"""),
            Regex("""आधा घंटा"""),
            Regex("""ek ghanta"""),
            Regex("""एक घंटा"""),
            Regex("""an hour""")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.value
        }
        return null
    }

    private fun extractTime(text: String): Triple<Int, Int, String?> {
        // Pattern 1: e.g. "at 7:30 AM", "7 am", "by 5 PM", "11:45 am"
        val amPmPattern = Regex("""\b(?:at|by|around\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)\b""")
        val amPmMatch = amPmPattern.find(text)
        if (amPmMatch != null) {
            val hourRaw = amPmMatch.groupValues[1].toIntOrNull() ?: 7
            val minRaw = amPmMatch.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: 0
            val amPm = amPmMatch.groupValues[3].replace(".", "").lowercase(Locale.ROOT)

            var hour = hourRaw % 12
            if (amPm == "pm") hour += 12

            return Triple(hour, minRaw.coerceIn(0, 59), amPmMatch.value)
        }

        // Pattern 2: Hindi/Hinglish "baje" or "बजे" e.g. "subah 7 baje", "shaam 5 baje", "7 baje", "रात 8 बजे"
        val bajePattern = Regex("""\b(?:subah|shaam|dopahar|raat|सुबह|शाम|दोपहर|रात)?\s*(\d{1,2})(?::(\d{2}))?\s*(?:baje|बजे)\b""")
        val bajeMatch = bajePattern.find(text)
        if (bajeMatch != null) {
            val hourRaw = bajeMatch.groupValues[1].toIntOrNull() ?: 7
            val minRaw = bajeMatch.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: 0
            val fullMatch = bajeMatch.value
            val isMorning = fullMatch.contains("subah") || fullMatch.contains("सुबह") || text.contains("subah") || text.contains("सुबह")
            val isEvening = fullMatch.contains("shaam") || fullMatch.contains("शाम") || text.contains("shaam") || text.contains("शाम")
            val isAfternoon = fullMatch.contains("dopahar") || fullMatch.contains("दोपहर") || text.contains("dopahar") || text.contains("दोपहर")
            val isNight = fullMatch.contains("raat") || fullMatch.contains("रात") || text.contains("raat") || text.contains("रात")

            var hour = hourRaw % 12
            if (isEvening || isNight || isAfternoon || (hourRaw in 1..6 && !isMorning)) {
                hour += 12
            }
            return Triple(hour, minRaw.coerceIn(0, 59), fullMatch)
        }

        // Pattern 3: "at 7 o'clock", "7 o'clock in the morning", "7 o'clock in the evening"
        val oclockPattern = Regex("""\b(?:at|by\s+)?(\d{1,2})\s*o['\s]?clock(?:\s*(in the morning|in the evening|in the afternoon|at night))?\b""")
        val oclockMatch = oclockPattern.find(text)
        if (oclockMatch != null) {
            val hourRaw = oclockMatch.groupValues[1].toIntOrNull() ?: 7
            val period = oclockMatch.groupValues[2].lowercase(Locale.ROOT)
            var hour = hourRaw % 12
            if (period.contains("evening") || period.contains("night") || period.contains("afternoon")) {
                hour += 12
            }
            return Triple(hour, 0, oclockMatch.value)
        }

        // Pattern 4: "at 17:00", "at 7:30"
        val colonPattern = Regex("""\b(?:at|by\s+)?(\d{1,2}):(\d{2})\b""")
        val colonMatch = colonPattern.find(text)
        if (colonMatch != null) {
            val hourRaw = colonMatch.groupValues[1].toIntOrNull() ?: 18
            val minRaw = colonMatch.groupValues[2].toIntOrNull() ?: 0
            val hour = if (hourRaw in 1..7) hourRaw + 12 else hourRaw
            return Triple(hour.coerceIn(0, 23), minRaw.coerceIn(0, 59), colonMatch.value)
        }

        // Pattern 5: General time of day terms without numeric hour
        if (text.contains("in the morning") || text.contains("subah") || text.contains("सुबह")) return Triple(8, 0, "in the morning")
        if (text.contains("in the afternoon") || text.contains("dopahar") || text.contains("दोपहर")) return Triple(14, 0, "in the afternoon")
        if (text.contains("in the evening") || text.contains("shaam") || text.contains("शाम")) return Triple(18, 0, "in the evening")
        if (text.contains("tonight") || text.contains("raat ko") || text.contains("रात को")) return Triple(20, 0, "tonight")

        // Default to a comfortable study time: 18:00 (6:00 PM)
        return Triple(18, 0, null)
    }

    private fun extractDate(text: String, hour: Int, minute: Int, zoneId: ZoneId): Triple<LocalDate, String, String?> {
        val today = LocalDate.now(zoneId)

        if (text.contains("tomorrow") || text.contains("kal") || text.contains("कल")) {
            val matched = when {
                text.contains("tomorrow") -> "tomorrow"
                text.contains("kal") -> "kal"
                else -> "कल"
            }
            return Triple(today.plusDays(1), "Tomorrow", matched)
        }

        if (text.contains("day after tomorrow") || text.contains("parso") || text.contains("परसों")) {
            val matched = when {
                text.contains("day after tomorrow") -> "day after tomorrow"
                text.contains("parso") -> "parso"
                else -> "परसों"
            }
            return Triple(today.plusDays(2), "In 2 Days", matched)
        }

        if (text.contains("today") || text.contains("tonight") || text.contains("aaj") || text.contains("आज")) {
            val matched = when {
                text.contains("today") -> "today"
                text.contains("tonight") -> "tonight"
                text.contains("aaj") -> "aaj"
                else -> "आज"
            }
            return Triple(today, "Today", matched)
        }

        // Weekdays in English & Hindi/Hinglish
        val weekdays = mapOf(
            "monday" to DayOfWeek.MONDAY,
            "somwar" to DayOfWeek.MONDAY,
            "सोमवार" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "mangalwar" to DayOfWeek.TUESDAY,
            "मंगलवार" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "budhwar" to DayOfWeek.WEDNESDAY,
            "बुधवार" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "guruwar" to DayOfWeek.THURSDAY,
            "veervar" to DayOfWeek.THURSDAY,
            "गुरुवार" to DayOfWeek.THURSDAY,
            "वीरवार" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "shukrawar" to DayOfWeek.FRIDAY,
            "शुक्रवार" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "shaniwar" to DayOfWeek.SATURDAY,
            "शनिवार" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY,
            "raviwar" to DayOfWeek.SUNDAY,
            "itwar" to DayOfWeek.SUNDAY,
            "रविवार" to DayOfWeek.SUNDAY
        )

        for ((dayName, dayEnum) in weekdays) {
            val dayRegex = Regex("""\b(?:on\s+)?(?:this\s+|next\s+)?$dayName\b""")
            val match = dayRegex.find(text)
            if (match != null) {
                var candidate = today.plusDays(1)
                while (candidate.dayOfWeek != dayEnum) {
                    candidate = candidate.plusDays(1)
                }
                val label = candidate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                return Triple(candidate, label, match.value)
            }
        }

        // If time is already past today, suggest tomorrow, else today
        val now = LocalTime.now(zoneId)
        val targetTime = LocalTime.of(hour, minute)
        return if (targetTime.isBefore(now)) {
            Triple(today.plusDays(1), "Tomorrow", null)
        } else {
            Triple(today, "Today", null)
        }
    }

    private fun extractSubject(text: String): String {
        for ((subjectName, keywords) in SUBJECT_KEYWORDS) {
            for (kw in keywords) {
                if (text.contains(kw)) {
                    return subjectName
                }
            }
        }
        return "Mathematics" // Default academic subject if general
    }

    private fun extractPriority(text: String): Pair<String, String?> {
        val highWords = listOf(
            "urgent", "high priority", "important", "asap", "exam", "test", "critical", "must do",
            "zaroori", "zaruri", "bahut zaroori", "जरूरी", "इम्तिहान", "परीक्षा"
        )
        for (w in highWords) {
            val pattern = Regex("""\b$w\b""")
            val match = pattern.find(text)
            if (match != null) {
                return Pair("High", match.value)
            }
        }

        val lowWords = listOf("low priority", "when free", "casual", "optional", "low", "aasan", "easy")
        for (w in lowWords) {
            val pattern = Regex("""\b$w\b""")
            val match = pattern.find(text)
            if (match != null) {
                return Pair("Low", match.value)
            }
        }

        return Pair("Medium", null)
    }

    private fun extractCleanTitle(
        original: String,
        tokensToRemove: List<String>,
        subject: String
    ): String {
        var clean = original

        // Remove matched token substrings
        for (token in tokensToRemove) {
            if (token.isNotBlank()) {
                clean = clean.replace(Regex("""(?i)\b${Regex.escape(token)}\b"""), " ")
            }
        }

        // Remove common conversational voice prefixes in English & Hindi/Hinglish
        val prefixPatterns = listOf(
            Regex("""(?i)^remind me to\s+"""),
            Regex("""(?i)^i need to\s+"""),
            Regex("""(?i)^i want to\s+"""),
            Regex("""(?i)^please schedule\s+"""),
            Regex("""(?i)^please add\s+"""),
            Regex("""(?i)^schedule\s+"""),
            Regex("""(?i)^add task\s+"""),
            Regex("""(?i)^add\s+"""),
            Regex("""(?i)^can you\s+"""),
            Regex("""(?i)^don't forget to\s+"""),
            Regex("""(?i)^dont forget to\s+"""),
            Regex("""(?i)^mujhe\s+"""),
            Regex("""(?i)^mera\s+"""),
            Regex("""(?i)^hamara\s+"""),
            Regex("""(?i)^kripya\s+""")
        )
        for (p in prefixPatterns) {
            clean = clean.replace(p, "")
        }

        // Remove common conversational Hindi & English suffixes
        val suffixPatterns = listOf(
            Regex("""(?i)\s+(?:padhna hai|karna hai|revise karna hai|complete karna hai|likhna hai|padho|karo|ke liye|ka homework)$"""),
            Regex("""(?i)\s+(?:पढ़ना है|करना है|रिवाइज करना है|लिखना है|के लिए|का होमवर्क)$"""),
            Regex("""(?i)\s+(?:for study|study task|homework)$""")
        )
        for (s in suffixPatterns) {
            clean = clean.replace(s, "")
        }

        // Clean extra whitespace and punctuation
        clean = clean.replace(Regex("""\s+"""), " ")
            .replace(Regex("""^[,\s\-:]+|[,\s\-:]+$"""), "")
            .trim()

        if (clean.isBlank() || clean.length < 3) {
            return "Revise $subject"
        }

        // Capitalize first character
        return clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private fun calculateXp(durationMinutes: Int, priority: String): Int {
        val base = when {
            durationMinutes >= 60 -> 40
            durationMinutes >= 45 -> 35
            durationMinutes >= 30 -> 25
            else -> 20
        }
        val priorityBonus = when (priority) {
            "High" -> 10
            "Low" -> -5
            else -> 0
        }
        return (base + priorityBonus).coerceIn(15, 100)
    }
}
