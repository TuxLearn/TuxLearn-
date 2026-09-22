package com.example

import com.example.data.InitialData
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyTask
import com.example.data.model.WeakAreaTopic
import com.example.util.ExamReadinessCalculator
import com.example.util.ReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamReadinessSystemTest {

    @Test
    fun testInitialSubjectReadinessValuesMatchTargets() {
        val exams = InitialData.sampleExams
        val tasks = InitialData.sampleTasks
        val flashcards = InitialData.sampleFlashcards
        val weakAreas = InitialData.sampleWeakAreas

        val allSubjects = ExamReadinessCalculator.calculateAllSubjects(exams, tasks, flashcards, weakAreas)

        val biology = allSubjects.firstOrNull { it.subject == "Biology" }
        assertNotNull("Biology subject should be present", biology)
        assertTrue(
            "Biology readiness should be around 82% (was ${biology!!.readinessPercentage}%)",
            biology.readinessPercentage in 80..85
        )
        assertEquals(ReadinessLevel.EXAM_READY, biology.level)

        val chemistry = allSubjects.firstOrNull { it.subject == "Chemistry" }
        assertNotNull("Chemistry subject should be present", chemistry)
        assertTrue(
            "Chemistry readiness should be around 64% (was ${chemistry!!.readinessPercentage}%)",
            chemistry.readinessPercentage in 60..68
        )
        assertEquals(ReadinessLevel.ALMOST_READY, chemistry.level)

        val physics = allSubjects.firstOrNull { it.subject == "Physics" }
        assertNotNull("Physics subject should be present", physics)
        assertTrue(
            "Physics readiness should be around 48% (was ${physics!!.readinessPercentage}%)",
            physics.readinessPercentage in 44..52
        )
        assertEquals(ReadinessLevel.GETTING_STARTED, physics.level)

        val math = allSubjects.firstOrNull { it.subject == "Mathematics" }
        assertNotNull("Mathematics subject should be present", math)
        assertTrue(
            "Mathematics readiness should be around 71% (was ${math!!.readinessPercentage}%)",
            math.readinessPercentage in 68..75
        )
        assertEquals(ReadinessLevel.ALMOST_READY, math.level)
    }

    @Test
    fun testReadinessLevelsThresholds() {
        assertEquals(ReadinessLevel.NEEDS_ATTENTION, ReadinessLevel.fromScore(0))
        assertEquals(ReadinessLevel.NEEDS_ATTENTION, ReadinessLevel.fromScore(39))
        assertEquals(ReadinessLevel.GETTING_STARTED, ReadinessLevel.fromScore(40))
        assertEquals(ReadinessLevel.GETTING_STARTED, ReadinessLevel.fromScore(59))
        assertEquals(ReadinessLevel.ALMOST_READY, ReadinessLevel.fromScore(60))
        assertEquals(ReadinessLevel.ALMOST_READY, ReadinessLevel.fromScore(79))
        assertEquals(ReadinessLevel.EXAM_READY, ReadinessLevel.fromScore(80))
        assertEquals(ReadinessLevel.EXAM_READY, ReadinessLevel.fromScore(100))
    }

    @Test
    fun testTaskCompletionIncreasesReadiness() {
        val exam = listOf(
            ExamItem(
                id = 1,
                subject = "Physics",
                examName = "Midterm",
                examDate = "Tomorrow",
                daysLeft = 1,
                syllabusTotalTopics = 10,
                syllabusCoveredTopics = 5,
                confidenceLevel = 3,
                targetMarks = 90
            )
        )
        val pendingTask = StudyTask(
            id = 1,
            title = "Physics: Solve Electromagnetism problems",
            subject = "Physics",
            dueDate = "Today",
            estimatedMinutes = 45,
            isCompleted = false,
            priority = "High"
        )
        val initialData = ExamReadinessCalculator.calculate("Physics", exam, listOf(pendingTask), emptyList(), emptyList())

        // Now student completes the study task
        val completedTask = pendingTask.copy(isCompleted = true, completedAt = System.currentTimeMillis())
        val updatedData = ExamReadinessCalculator.calculate("Physics", exam, listOf(completedTask), emptyList(), emptyList())

        assertTrue(
            "Readiness should increase after completing task (before: ${initialData.readinessPercentage}, after: ${updatedData.readinessPercentage})",
            updatedData.readinessPercentage > initialData.readinessPercentage
        )
    }

    @Test
    fun testFlashcardMasteryIncreasesReadiness() {
        val card = FlashcardItem(
            id = 1,
            subject = "Chemistry",
            question = "What is a buffer solution?",
            answer = "A solution that resists pH changes.",
            hint = "Weak acid and conjugate base",
            isMastered = false,
            reviewCount = 1
        )
        val initialData = ExamReadinessCalculator.calculate("Chemistry", emptyList(), emptyList(), listOf(card), emptyList())

        // Student marks card as mastered
        val masteredCard = card.copy(isMastered = true, reviewCount = 2)
        val updatedData = ExamReadinessCalculator.calculate("Chemistry", emptyList(), emptyList(), listOf(masteredCard), emptyList())

        assertTrue(
            "Readiness should increase after mastering revision card (before: ${initialData.readinessPercentage}, after: ${updatedData.readinessPercentage})",
            updatedData.readinessPercentage > initialData.readinessPercentage
        )
    }

    @Test
    fun testResolvingWeakAreaIncreasesReadiness() {
        val weakArea = WeakAreaTopic(
            id = 1,
            subject = "Mathematics",
            topicName = "Integration by parts",
            isResolved = false,
            priority = "High"
        )
        val initialData = ExamReadinessCalculator.calculate("Mathematics", emptyList(), emptyList(), emptyList(), listOf(weakArea))

        // Student resolves weak area
        val resolvedWeakArea = weakArea.copy(isResolved = true)
        val updatedData = ExamReadinessCalculator.calculate("Mathematics", emptyList(), emptyList(), emptyList(), listOf(resolvedWeakArea))

        assertTrue(
            "Readiness should increase after resolving weak area (before: ${initialData.readinessPercentage}, after: ${updatedData.readinessPercentage})",
            updatedData.readinessPercentage > initialData.readinessPercentage
        )
    }

    @Test
    fun testSubjectDetailsContainsRequiredFields() {
        val exams = InitialData.sampleExams
        val tasks = InitialData.sampleTasks
        val flashcards = InitialData.sampleFlashcards
        val weakAreas = InitialData.sampleWeakAreas

        val data = ExamReadinessCalculator.calculate("Biology", exams, tasks, flashcards, weakAreas)

        // Verify all required details exist
        assertTrue("Topics completed must be >= 0", data.topicsCompleted > 0)
        assertTrue("Topics remaining must be >= 0", data.topicsRemaining >= 0)
        assertEquals("Total topics must match", data.totalTopics, data.topicsCompleted + data.topicsRemaining)
        assertFalse("Strong topics should not be empty", data.strongTopics.isEmpty())
        assertNotNull("Suggested next task must be present", data.suggestedNextTask)
        assertTrue("Suggested task must not be blank", data.suggestedNextTask.isNotBlank())
        assertNotNull("Last studied date must be present", data.lastStudiedDate)
        assertTrue("Last studied date must not be blank", data.lastStudiedDate.isNotBlank())
    }
}
