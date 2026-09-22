package com.example

import com.example.util.VoiceTaskParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class VoiceHomeworkEntryTest {

    private val testZoneId = ZoneId.of("UTC")

    @Test
    fun testUserPromptExactExample() {
        val input = "Tomorrow at 7 AM revise Physics Chapter 3 for 30 minutes."
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Physics", result.subject)
        assertEquals(30, result.durationMinutes)
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
        assertEquals("Tomorrow", result.dueDate)
        assertEquals("Medium", result.priority)
        assertEquals(25, result.xpReward)
        assertTrue(result.title.contains("Physics Chapter 3", ignoreCase = true))
        assertNotNull(result.reminderEpochMillis)
        assertTrue(result.reminderEnabled)
    }

    @Test
    fun testHighPriorityAndUrgentDetection() {
        val input = "Today at 5 PM solve Chemistry stoichiometry worksheet for 45 minutes urgent"
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Chemistry", result.subject)
        assertEquals(45, result.durationMinutes)
        assertEquals(17, result.hour)
        assertEquals(0, result.minute)
        assertEquals("Today", result.dueDate)
        assertEquals("High", result.priority)
        assertEquals(45, result.xpReward) // 35 base + 10 high priority
        assertTrue(result.title.contains("Chemistry stoichiometry worksheet", ignoreCase = true))
    }

    @Test
    fun testHourDurationAndMathsSubject() {
        val input = "Friday at 6 PM complete Maths calculus problem set for 1 hour"
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Mathematics", result.subject)
        assertEquals(60, result.durationMinutes)
        assertEquals(18, result.hour)
        assertEquals(0, result.minute)
        assertEquals(40, result.xpReward)
        assertTrue(result.title.contains("Maths calculus problem set", ignoreCase = true))
    }

    @Test
    fun testBiologyAndTonightDetection() {
        val input = "Tonight at 8 PM read Biology cell division chapter for 25 minutes"
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Biology", result.subject)
        assertEquals(25, result.durationMinutes)
        assertEquals(20, result.hour)
        assertEquals(0, result.minute)
        assertEquals(20, result.xpReward)
        assertTrue(result.title.contains("Biology cell division chapter", ignoreCase = true))
    }

    @Test
    fun testComputerScienceQuickSort() {
        val input = "Tomorrow at 4 PM practice Computer Science QuickSort algorithms for 40 minutes"
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Computer Science", result.subject)
        assertEquals(40, result.durationMinutes)
        assertEquals(16, result.hour)
        assertEquals(0, result.minute)
        assertEquals("Tomorrow", result.dueDate)
        assertTrue(result.title.contains("QuickSort algorithms", ignoreCase = true))
    }

    @Test
    fun testHindiHinglishCommand() {
        val input = "kal subah 7 baje Physics revise karo 30 minute ke liye"
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Physics", result.subject)
        assertEquals(30, result.durationMinutes)
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
        assertEquals("Tomorrow", result.dueDate)
        assertTrue(result.reminderEnabled)
    }

    @Test
    fun testHindiDevanagariCommand() {
        val input = "कल सुबह 7 बजे गणित 45 मिनट के लिए पढ़ना है"
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Mathematics", result.subject)
        assertEquals(45, result.durationMinutes)
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
        assertEquals("Tomorrow", result.dueDate)
    }

    @Test
    fun testHinglishTodayEveningHourCommand() {
        val input = "aaj shaam 5 baje Chemistry 1 ghanta padho urgent"
        val result = VoiceTaskParser.parse(input, testZoneId)

        assertEquals("Chemistry", result.subject)
        assertEquals(60, result.durationMinutes)
        assertEquals(17, result.hour)
        assertEquals(0, result.minute)
        assertEquals("Today", result.dueDate)
        assertEquals("High", result.priority)
    }
}
