package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialData
import com.example.data.model.SubjectMark
import com.example.data.repository.StudoraRepository
import com.example.ui.screens.formatMarksNumber
import com.example.ui.screens.getGradeForPercentage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MarksPercentageCalculatorTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: StudoraRepository

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        database.studentProfileDao().insertOrUpdate(InitialData.sampleProfile)
        database.subjectMarkDao().insertAll(InitialData.sampleMarks)
        repository = StudoraRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInitialSampleMarksAndCalculation() = runBlocking {
        val marks = repository.allSubjectMarks.first()
        assertEquals(4, marks.size)

        val physics = marks.find { it.subjectName == "Physics" }
        assertEquals(72.0, physics?.obtainedMarks ?: 0.0, 0.001)
        assertEquals(100.0, physics?.maxMarks ?: 0.0, 0.001)

        val chem = marks.find { it.subjectName == "Chemistry" }
        assertEquals(81.0, chem?.obtainedMarks ?: 0.0, 0.001)
        assertEquals(100.0, chem?.maxMarks ?: 0.0, 0.001)

        val bio = marks.find { it.subjectName == "Biology" }
        assertEquals(88.0, bio?.obtainedMarks ?: 0.0, 0.001)
        assertEquals(100.0, bio?.maxMarks ?: 0.0, 0.001)

        val math = marks.find { it.subjectName == "Mathematics" }
        assertEquals(76.0, math?.obtainedMarks ?: 0.0, 0.001)
        assertEquals(100.0, math?.maxMarks ?: 0.0, 0.001)

        val totalMax = marks.sumOf { it.maxMarks }
        val totalObtained = marks.sumOf { it.obtainedMarks }
        assertEquals(400.0, totalMax, 0.001)
        assertEquals(317.0, totalObtained, 0.001)

        val percentage = (totalObtained / totalMax) * 100.0
        assertEquals(79.25, percentage, 0.001)

        val grade = getGradeForPercentage(percentage)
        assertEquals("B+", grade)

        val isPass = percentage >= 40.0
        assertTrue("Overall result should be Pass", isPass)
    }

    @Test
    fun testAddSubjectAndAutoRecalculation() = runBlocking {
        val newMark = SubjectMark(
            subjectName = "Computer Science",
            maxMarks = 100.0,
            obtainedMarks = 95.0
        )
        repository.insertSubjectMark(newMark)

        val updatedMarks = repository.allSubjectMarks.first()
        assertEquals(5, updatedMarks.size)

        val totalMax = updatedMarks.sumOf { it.maxMarks }
        val totalObtained = updatedMarks.sumOf { it.obtainedMarks }
        assertEquals(500.0, totalMax, 0.001)
        assertEquals(412.0, totalObtained, 0.001)

        val percentage = (totalObtained / totalMax) * 100.0
        assertEquals(82.4, percentage, 0.001)

        val grade = getGradeForPercentage(percentage)
        assertEquals("A", grade)
        assertTrue(percentage >= 40.0)
    }

    @Test
    fun testEditSubject() = runBlocking {
        val initialMarks = repository.allSubjectMarks.first()
        val physics = initialMarks.first { it.subjectName == "Physics" }

        val editedPhysics = physics.copy(obtainedMarks = 92.0)
        repository.updateSubjectMark(editedPhysics)

        val updatedMarks = repository.allSubjectMarks.first()
        val recheckedPhysics = updatedMarks.first { it.id == physics.id }
        assertEquals(92.0, recheckedPhysics.obtainedMarks, 0.001)

        val totalObtained = updatedMarks.sumOf { it.obtainedMarks }
        assertEquals(337.0, totalObtained, 0.001)
        val percentage = (totalObtained / 400.0) * 100.0
        assertEquals(84.25, percentage, 0.001)
        assertEquals("A", getGradeForPercentage(percentage))
    }

    @Test
    fun testDeleteSubject() = runBlocking {
        val initialMarks = repository.allSubjectMarks.first()
        val math = initialMarks.first { it.subjectName == "Mathematics" }

        repository.deleteSubjectMark(math)

        val updatedMarks = repository.allSubjectMarks.first()
        assertEquals(3, updatedMarks.size)
        assertFalse(updatedMarks.any { it.subjectName == "Mathematics" })

        val totalMax = updatedMarks.sumOf { it.maxMarks }
        val totalObtained = updatedMarks.sumOf { it.obtainedMarks }
        assertEquals(300.0, totalMax, 0.001)
        assertEquals(241.0, totalObtained, 0.001)
    }

    @Test
    fun testClearAllSubjectsAndReset() = runBlocking {
        repository.clearAllSubjectMarks()

        val emptyMarks = repository.allSubjectMarks.first()
        assertTrue(emptyMarks.isEmpty())

        val totalMax = emptyMarks.sumOf { it.maxMarks }
        val totalObtained = emptyMarks.sumOf { it.obtainedMarks }
        val percentage = if (totalMax > 0.0) (totalObtained / totalMax) * 100.0 else 0.0
        assertEquals(0.0, percentage, 0.001)
    }

    @Test
    fun testGradeScaleBoundaries() {
        // 90–100 = A+
        assertEquals("A+", getGradeForPercentage(100.0))
        assertEquals("A+", getGradeForPercentage(95.5))
        assertEquals("A+", getGradeForPercentage(90.0))

        // 80–89 = A
        assertEquals("A", getGradeForPercentage(89.9))
        assertEquals("A", getGradeForPercentage(85.0))
        assertEquals("A", getGradeForPercentage(80.0))

        // 70–79 = B+
        assertEquals("B+", getGradeForPercentage(79.9))
        assertEquals("B+", getGradeForPercentage(75.0))
        assertEquals("B+", getGradeForPercentage(70.0))

        // 60–69 = B
        assertEquals("B", getGradeForPercentage(69.9))
        assertEquals("B", getGradeForPercentage(65.0))
        assertEquals("B", getGradeForPercentage(60.0))

        // 50–59 = C
        assertEquals("C", getGradeForPercentage(59.9))
        assertEquals("C", getGradeForPercentage(55.0))
        assertEquals("C", getGradeForPercentage(50.0))

        // 40–49 = D
        assertEquals("D", getGradeForPercentage(49.9))
        assertEquals("D", getGradeForPercentage(45.0))
        assertEquals("D", getGradeForPercentage(40.0))

        // Below 40 = F
        assertEquals("F", getGradeForPercentage(39.9))
        assertEquals("F", getGradeForPercentage(25.0))
        assertEquals("F", getGradeForPercentage(0.0))
    }

    @Test
    fun testPassFailThreshold() {
        val passThreshold = 40.0

        assertTrue(100.0 >= passThreshold)
        assertTrue(79.25 >= passThreshold)
        assertTrue(40.0 >= passThreshold)

        assertFalse(39.9 >= passThreshold)
        assertFalse(25.0 >= passThreshold)
        assertFalse(0.0 >= passThreshold)
    }

    @Test
    fun testValidationRules() {
        fun validateSubjectEntry(name: String, max: Double?, obtained: Double?): Boolean {
            val isNameEmpty = name.trim().isEmpty()
            val isMaxInvalid = max == null || max <= 0.0
            val isObtainedMissing = obtained == null
            val isObtainedNegative = obtained != null && obtained < 0.0
            val isObtainedExceedsMax = max != null && obtained != null && obtained > max
            return !isNameEmpty && !isMaxInvalid && !isObtainedMissing && !isObtainedNegative && !isObtainedExceedsMax
        }

        // Valid inputs
        assertTrue(validateSubjectEntry("Physics", 100.0, 72.0))
        assertTrue(validateSubjectEntry("Biology", 50.0, 45.0))
        assertTrue(validateSubjectEntry("Math", 100.0, 0.0)) // 0 marks obtained is valid
        assertTrue(validateSubjectEntry("History", 100.0, 100.0)) // full marks is valid

        // Invalid: empty subject name
        assertFalse(validateSubjectEntry("  ", 100.0, 72.0))
        assertFalse(validateSubjectEntry("", 100.0, 72.0))

        // Invalid: max marks <= 0
        assertFalse(validateSubjectEntry("Physics", 0.0, 0.0))
        assertFalse(validateSubjectEntry("Physics", -10.0, 5.0))

        // Invalid: marks obtained negative
        assertFalse(validateSubjectEntry("Physics", 100.0, -5.0))

        // Invalid: marks obtained exceeds max marks
        assertFalse(validateSubjectEntry("Physics", 100.0, 105.0))
        assertFalse(validateSubjectEntry("Chemistry", 50.0, 51.0))
    }

    @Test
    fun testFormattingMarks() {
        assertEquals("100", formatMarksNumber(100.0))
        assertEquals("72", formatMarksNumber(72.0))
        assertEquals("72.5", formatMarksNumber(72.5))
        assertEquals("88.8", formatMarksNumber(88.8))
    }
}
