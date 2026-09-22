package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialData
import com.example.data.model.TimetableSlot
import com.example.data.repository.StudoraRepository
import com.example.ui.screens.DAYS_OF_WEEK
import com.example.ui.screens.checkTimeConflict
import com.example.ui.screens.getCategoryColor
import com.example.ui.screens.getShortDayLabel
import com.example.ui.screens.getTimetableCategoryColor
import com.example.util.DeviceTimeService
import com.example.util.parseTimeStringToMinutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudentTimetableTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: StudoraRepository

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        database.studentProfileDao().insertOrUpdate(InitialData.sampleProfile)
        database.timetableDao().insertAll(InitialData.sampleTimetable)
        repository = StudoraRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWeeklyDaysCountAndLabels() {
        assertEquals(7, DAYS_OF_WEEK.size)
        val expectedDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        assertEquals(expectedDays, DAYS_OF_WEEK)

        assertEquals("Mon", getShortDayLabel("Monday"))
        assertEquals("Tue", getShortDayLabel("Tuesday"))
        assertEquals("Wed", getShortDayLabel("Wednesday"))
        assertEquals("Thu", getShortDayLabel("Thursday"))
        assertEquals("Fri", getShortDayLabel("Friday"))
        assertEquals("Sat", getShortDayLabel("Saturday"))
        assertEquals("Sun", getShortDayLabel("Sunday"))
    }

    @Test
    fun testChronologicalOrderingOfClasses() {
        val slots = listOf(
            TimetableSlot(id = 1, dayOfWeek = "Monday", startTime = "01:00 PM", endTime = "02:30 PM", subject = "Biology"),
            TimetableSlot(id = 2, dayOfWeek = "Monday", startTime = "08:30 AM", endTime = "09:45 AM", subject = "Physics"),
            TimetableSlot(id = 3, dayOfWeek = "Monday", startTime = "11:15 AM", endTime = "12:30 PM", subject = "Chemistry"),
            TimetableSlot(id = 4, dayOfWeek = "Monday", startTime = "10:00 AM", endTime = "11:00 AM", subject = "Mathematics")
        )

        val sorted = slots.sortedBy { parseTimeStringToMinutes(it.startTime) }

        assertEquals("Physics", sorted[0].subject) // 08:30 AM
        assertEquals("Mathematics", sorted[1].subject) // 10:00 AM
        assertEquals("Chemistry", sorted[2].subject) // 11:15 AM
        assertEquals("Biology", sorted[3].subject) // 01:00 PM
    }

    @Test
    fun testAddTimetableEntryWithAllFields() = runBlocking {
        val newSlot = TimetableSlot(
            dayOfWeek = "Saturday",
            startTime = "10:00 AM",
            endTime = "11:30 AM",
            subject = "Computer Networks",
            teacherName = "Prof. Turing",
            roomOrClass = "Lab 402",
            colorCategory = "Cyan",
            note = "Bring project code on USB",
            isStudySession = false,
            reminderEnabled = true
        )

        val id = repository.insertTimetableSlot(newSlot)
        assertTrue(id > 0)

        val allSlots = repository.allTimetableSlots.first()
        val saved = allSlots.find { it.id == id.toInt() }

        assertNotNull(saved)
        assertEquals("Saturday", saved?.dayOfWeek)
        assertEquals("10:00 AM", saved?.startTime)
        assertEquals("11:30 AM", saved?.endTime)
        assertEquals("Computer Networks", saved?.subject)
        assertEquals("Prof. Turing", saved?.teacherName)
        assertEquals("Lab 402", saved?.roomOrClass)
        assertEquals("Cyan", saved?.colorCategory)
        assertEquals("Bring project code on USB", saved?.note)
        assertTrue(saved?.reminderEnabled == true)
    }

    @Test
    fun testEditTimetableEntry() = runBlocking {
        val initialSlots = repository.allTimetableSlots.first()
        val firstSlot = initialSlots.first()

        val updated = firstSlot.copy(
            subject = "Advanced Mathematics",
            startTime = "08:45 AM",
            roomOrClass = "Auditorium A",
            colorCategory = "Purple"
        )
        repository.updateTimetableSlot(updated)

        val recheckedSlots = repository.allTimetableSlots.first()
        val found = recheckedSlots.first { it.id == firstSlot.id }

        assertEquals("Advanced Mathematics", found.subject)
        assertEquals("08:45 AM", found.startTime)
        assertEquals("Auditorium A", found.roomOrClass)
        assertEquals("Purple", found.colorCategory)
    }

    @Test
    fun testDeleteTimetableEntry() = runBlocking {
        val initialSlots = repository.allTimetableSlots.first()
        val countBefore = initialSlots.size
        val toDelete = initialSlots.first()

        repository.deleteTimetableSlot(toDelete)

        val afterSlots = repository.allTimetableSlots.first()
        assertEquals(countBefore - 1, afterSlots.size)
        assertFalse(afterSlots.any { it.id == toDelete.id })
    }

    @Test
    fun testConflictDetectionOverlappingSlots() {
        val existing = listOf(
            TimetableSlot(id = 1, dayOfWeek = "Monday", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Mathematics"),
            TimetableSlot(id = 2, dayOfWeek = "Monday", startTime = "11:00 AM", endTime = "12:15 PM", subject = "Physics"),
            TimetableSlot(id = 3, dayOfWeek = "Tuesday", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Chemistry")
        )

        // Conflict: 10:00 AM - 11:00 AM overlaps with 09:00 AM - 10:15 AM
        val conflict1 = checkTimeConflict(existing, "Monday", "10:00 AM", "11:00 AM")
        assertNotNull(conflict1)
        assertEquals("Mathematics", conflict1?.subject)

        // Conflict: Inside 09:15 AM - 10:00 AM
        val conflict2 = checkTimeConflict(existing, "Monday", "09:15 AM", "10:00 AM")
        assertNotNull(conflict2)
        assertEquals("Mathematics", conflict2?.subject)

        // No conflict: Adjacent session 10:15 AM - 11:00 AM
        val noConflictAdjacent = checkTimeConflict(existing, "Monday", "10:15 AM", "11:00 AM")
        assertNull(noConflictAdjacent)

        // No conflict: Different day (Tuesday) at 11:00 AM
        val noConflictOtherDay = checkTimeConflict(existing, "Tuesday", "11:00 AM", "12:00 PM")
        assertNull(noConflictOtherDay)

        // When editing existing slot (id = 1), it shouldn't conflict with itself
        val editSelfNoConflict = checkTimeConflict(existing, "Monday", "09:00 AM", "10:15 AM", currentSlotId = 1)
        assertNull(editSelfNoConflict)
    }

    @Test
    fun testCurrentAndNextClassCalculation() {
        val todaySlots = listOf(
            TimetableSlot(id = 1, dayOfWeek = "Monday", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Mathematics"),
            TimetableSlot(id = 2, dayOfWeek = "Monday", startTime = "10:30 AM", endTime = "11:45 AM", subject = "Physics"),
            TimetableSlot(id = 3, dayOfWeek = "Monday", startTime = "01:00 PM", endTime = "02:15 PM", subject = "Chemistry")
        )

        // Simulate 09:30 AM (570 minutes from midnight)
        val nowAt930 = 9 * 60 + 30
        val currentSlot = todaySlots.firstOrNull { slot ->
            val start = parseTimeStringToMinutes(slot.startTime)
            val end = parseTimeStringToMinutes(slot.endTime)
            start <= nowAt930 && nowAt930 < end
        }
        val nextSlot = todaySlots.firstOrNull { slot ->
            val start = parseTimeStringToMinutes(slot.startTime)
            start > nowAt930
        }

        assertNotNull(currentSlot)
        assertEquals("Mathematics", currentSlot?.subject)
        assertNotNull(nextSlot)
        assertEquals("Physics", nextSlot?.subject)
        assertEquals("10:30 AM", nextSlot?.startTime)

        val nextLabel = "Next: ${nextSlot?.subject} at ${nextSlot?.startTime}"
        assertEquals("Next: Physics at 10:30 AM", nextLabel)
    }

    @Test
    fun testTimeParsingFormats() {
        assertEquals(540, parseTimeStringToMinutes("09:00 AM"))
        assertEquals(540, parseTimeStringToMinutes("9:00 AM"))
        assertEquals(540, parseTimeStringToMinutes("9:00 am"))
        assertEquals(615, parseTimeStringToMinutes("10:15 AM"))
        assertEquals(780, parseTimeStringToMinutes("01:00 PM"))
        assertEquals(780, parseTimeStringToMinutes("1:00 PM"))
        assertEquals(780, parseTimeStringToMinutes("13:00"))
        assertEquals(870, parseTimeStringToMinutes("14:30"))
    }

    @Test
    fun testDeviceTimeServiceZoneAndLocalDate() {
        val zoneId = DeviceTimeService.currentZoneId()
        assertNotNull(zoneId)

        val localDate = DeviceTimeService.currentLocalDate()
        assertNotNull(localDate)

        val dayOfWeekName = DeviceTimeService.currentDayOfWeekName()
        assertTrue(DAYS_OF_WEEK.contains(dayOfWeekName))
    }
}
