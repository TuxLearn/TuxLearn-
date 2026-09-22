package com.example

import com.example.data.ActiveRecoveryPlan
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyTask
import com.example.util.FallingBehindDetector
import com.example.util.StudyPaceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FallingBehindDetectorTest {

    private val sampleExams = listOf(
        ExamItem(
            id = 1,
            subject = "Physics",
            examName = "Physics Unit 2",
            examDate = LocalDate.now().plusDays(2).toString(),
            daysLeft = 2,
            syllabusTotalTopics = 10,
            syllabusCoveredTopics = 4,
            confidenceLevel = 2,
            targetMarks = 90
        ),
        ExamItem(
            id = 2,
            subject = "Mathematics",
            examName = "Maths Midterm",
            examDate = LocalDate.now().plusDays(10).toString(),
            daysLeft = 10,
            syllabusTotalTopics = 8,
            syllabusCoveredTopics = 7,
            confidenceLevel = 4,
            targetMarks = 95
        )
    )

    private val sampleFlashcards = listOf(
        FlashcardItem(id = 1, subject = "Physics", question = "Ohm's Law", answer = "V = IR", isMastered = false),
        FlashcardItem(id = 2, subject = "Physics", question = "Work formula", answer = "W = Fd", isMastered = false)
    )

    @Test
    fun testOnTrackWhenNoTasksAreOverdue() {
        val tasks = listOf(
            StudyTask(id = 1, title = "Revise Physics", subject = "Physics", dueDate = "Today", estimatedMinutes = 30, priority = "Medium", isCompleted = true),
            StudyTask(id = 2, title = "Maths worksheet", subject = "Mathematics", dueDate = "Tomorrow", estimatedMinutes = 45, priority = "Low", isCompleted = false)
        )

        val assessment = FallingBehindDetector.assessStudyStatus(
            tasks = tasks,
            exams = listOf(sampleExams[1]), // Exam 10 days away
            flashcards = sampleFlashcards,
            activePlan = null
        )

        assertEquals(StudyPaceStatus.ON_TRACK, assessment.status)
        assertEquals(0, assessment.overdueTasks.size)
        assertTrue(assessment.overdueTasks.isEmpty())
    }

    @Test
    fun testSlightlyBehindWithSingleOverdueTask() {
        val tasks = listOf(
            StudyTask(
                id = 1,
                title = "Biology Cells Revision",
                subject = "Biology",
                dueDate = "Yesterday",
                estimatedMinutes = 30,
                priority = "Medium",
                isCompleted = false
            ),
            StudyTask(
                id = 2,
                title = "Maths Exercises",
                subject = "Mathematics",
                dueDate = "Tomorrow",
                estimatedMinutes = 40,
                priority = "Medium",
                isCompleted = false
            )
        )

        val assessment = FallingBehindDetector.assessStudyStatus(
            tasks = tasks,
            exams = listOf(sampleExams[1]),
            flashcards = emptyList(),
            activePlan = null
        )

        assertEquals(StudyPaceStatus.SLIGHTLY_BEHIND, assessment.status)
        assertEquals(1, assessment.overdueTasks.size)
    }

    @Test
    fun testFallingBehindWithMultipleOverdueTasksAndUpcomingExam() {
        val tasks = listOf(
            StudyTask(id = 1, title = "Electromagnetism practice", subject = "Physics", dueDate = "Yesterday", estimatedMinutes = 45, priority = "High", isCompleted = false),
            StudyTask(id = 2, title = "Optics ray diagram notes", subject = "Physics", dueDate = "2 days ago", estimatedMinutes = 30, priority = "High", isCompleted = false),
            StudyTask(id = 3, title = "Calculus limits questions", subject = "Mathematics", dueDate = "Yesterday", estimatedMinutes = 60, priority = "Medium", isCompleted = false)
        )

        val assessment = FallingBehindDetector.assessStudyStatus(
            tasks = tasks,
            exams = sampleExams, // Physics in 2 days
            flashcards = sampleFlashcards,
            activePlan = null
        )

        assertEquals(StudyPaceStatus.FALLING_BEHIND, assessment.status)
        assertEquals(3, assessment.overdueTasks.size)
    }

    @Test
    fun testGenerateRecoveryPlanDoesNotDumpAllTasksOnToday() {
        val overdueTasks = listOf(
            StudyTask(id = 1, title = "Physics 1", subject = "Physics", dueDate = "Yesterday", estimatedMinutes = 45, priority = "High"),
            StudyTask(id = 2, title = "Physics 2", subject = "Physics", dueDate = "Yesterday", estimatedMinutes = 30, priority = "High"),
            StudyTask(id = 3, title = "Maths 1", subject = "Mathematics", dueDate = "Yesterday", estimatedMinutes = 40, priority = "Medium"),
            StudyTask(id = 4, title = "Chemistry 1", subject = "Chemistry", dueDate = "Yesterday", estimatedMinutes = 35, priority = "Low")
        )

        val plan = FallingBehindDetector.generateRecoveryPlan(
            tasks = overdueTasks,
            exams = sampleExams,
            dailyPaceMinutes = 60,
            daysAvailable = 3
        )

        assertNotNull(plan)
        assertEquals(3, plan.daysPlanned)
        assertEquals(4, plan.totalTasksCount)
        assertEquals(150, plan.totalStudyMinutes)

        // Ensure tasks are distributed across days, NOT all on Day 1
        val day1Tasks = plan.days[0].tasks
        val day2Tasks = plan.days[1].tasks
        assertTrue("Day 1 should not contain all 4 tasks", day1Tasks.size < 4)
        assertTrue("Day 2 should have tasks", day2Tasks.isNotEmpty())

        // Ensure high priority physics task is prioritized on Day 1
        assertTrue(day1Tasks.any { it.task.subject == "Physics" })
    }

    @Test
    fun testActiveRecoveryPlanProgress() {
        val tasks = listOf(
            StudyTask(id = 10, title = "T1", subject = "Physics", dueDate = "Today", isCompleted = true),
            StudyTask(id = 11, title = "T2", subject = "Physics", dueDate = "Tomorrow", isCompleted = false)
        )

        val activePlan = ActiveRecoveryPlan(
            isActive = true,
            taskIds = listOf(10, 11),
            totalTasks = 2,
            daysPlanned = 3,
            totalStudyMinutes = 75,
            appliedEpoch = System.currentTimeMillis()
        )

        val assessment = FallingBehindDetector.assessStudyStatus(
            tasks = tasks,
            exams = emptyList(),
            flashcards = emptyList(),
            activePlan = activePlan
        )

        assertNotNull(assessment.activeRecoveryPlan)
        assertEquals(1, assessment.recoveredTasksCount)
        assertEquals(2, assessment.activeRecoveryPlan?.totalTasks)
    }

    @Test
    fun testSevenPendingTasksWithOverdueIsNotOnTrack() {
        val tasks = com.example.data.InitialData.sampleTasks
        assertEquals("InitialData sampleTasks has 8 tasks total (7 pending, 1 completed)", 8, tasks.size)

        val assessment = FallingBehindDetector.assessStudyStatus(
            tasks = tasks,
            exams = sampleExams,
            flashcards = sampleFlashcards,
            activePlan = null
        )

        // Verify the 7 pending tasks
        assertEquals(7, assessment.pendingTasks.size)
        // Verify overdue tasks (tasks with 'Yesterday')
        assertEquals(4, assessment.overdueTasks.size)

        // CRITICAL: The message "You're on track" is NOT displayed when overdue tasks actually exist!
        assertTrue("Status must NOT be ON_TRACK when overdue tasks exist", assessment.status != StudyPaceStatus.ON_TRACK)
        assertEquals(StudyPaceStatus.FALLING_BEHIND, assessment.status)
        assertFalse("Explanation should not say on track", assessment.explanation.contains("on track", ignoreCase = true))

        // Generating recovery plan must include all overdue tasks and distribute them
        val recoveryPlan = FallingBehindDetector.generateRecoveryPlan(
            tasks = assessment.pendingTasks,
            exams = sampleExams,
            dailyPaceMinutes = 90,
            daysAvailable = 3
        )
        assertNotNull(recoveryPlan)
        assertTrue(recoveryPlan.allPlannedTasks.isNotEmpty())
        assertTrue(recoveryPlan.allPlannedTasks.any { it.task.title.contains("Calculus") })
        // All planned tasks have valid new suggested date labels
        recoveryPlan.allPlannedTasks.forEach { planned ->
            assertNotNull(planned.scheduledDayLabel)
            assertTrue(planned.scheduledDayLabel.isNotBlank())
            assertTrue(planned.task.estimatedMinutes > 0)
        }
    }

    @Test
    fun testIsTaskOverdueVariousFormats() {
        val today = LocalDate.now()
        val yesterdayDateStr = today.minusDays(1).toString()
        val tomorrowDateStr = today.plusDays(1).toString()

        val taskYesterdayWord = StudyTask(id = 1, title = "T1", subject = "S", dueDate = "Yesterday", isCompleted = false)
        val taskYesterdayDate = StudyTask(id = 2, title = "T2", subject = "S", dueDate = yesterdayDateStr, isCompleted = false)
        val taskTodayWord = StudyTask(id = 3, title = "T3", subject = "S", dueDate = "Today", isCompleted = false)
        val taskTomorrowWord = StudyTask(id = 4, title = "T4", subject = "S", dueDate = "Tomorrow", isCompleted = false)
        val taskTomorrowDate = StudyTask(id = 5, title = "T5", subject = "S", dueDate = tomorrowDateStr, isCompleted = false)
        val taskCompletedYesterday = StudyTask(id = 6, title = "T6", subject = "S", dueDate = "Yesterday", isCompleted = true)

        assertTrue("Task with 'Yesterday' must be overdue", FallingBehindDetector.isTaskOverdue(taskYesterdayWord))
        assertTrue("Task with yesterday's ISO date must be overdue", FallingBehindDetector.isTaskOverdue(taskYesterdayDate))
        assertFalse("Task with 'Today' must not be overdue", FallingBehindDetector.isTaskOverdue(taskTodayWord))
        assertFalse("Task with 'Tomorrow' must not be overdue", FallingBehindDetector.isTaskOverdue(taskTomorrowWord))
        assertFalse("Task with tomorrow's date must not be overdue", FallingBehindDetector.isTaskOverdue(taskTomorrowDate))
        assertFalse("Completed task must never be overdue", FallingBehindDetector.isTaskOverdue(taskCompletedYesterday))
    }
}
