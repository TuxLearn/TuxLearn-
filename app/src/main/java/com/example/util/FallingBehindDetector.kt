package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.ActiveRecoveryPlan
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudyTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class StudyPaceStatus(
    val label: String,
    val primaryColor: Color,
    val containerColor: Color,
    val onContainerColor: Color
) {
    ON_TRACK(
        label = "On Track",
        primaryColor = Color(0xFF10B981), // Green
        containerColor = Color(0xFFD1FAE5),
        onContainerColor = Color(0xFF065F46)
    ),
    SLIGHTLY_BEHIND(
        label = "Slightly Behind",
        primaryColor = Color(0xFFF59E0B), // Amber
        containerColor = Color(0xFFFEF3C7),
        onContainerColor = Color(0xFF92400E)
    ),
    FALLING_BEHIND(
        label = "Falling Behind",
        primaryColor = Color(0xFFEF4444), // Red/Orange
        containerColor = Color(0xFFFEE2E2),
        onContainerColor = Color(0xFF991B1B)
    )
}

data class StudyStatusAssessment(
    val status: StudyPaceStatus,
    val title: String,
    val explanation: String,
    val overdueTasks: List<StudyTask>,
    val pendingTasks: List<StudyTask>,
    val upcomingImportantTasks: List<StudyTask>,
    val approachingExam: ExamItem?,
    val examDaysLeft: Int?,
    val lowestReadinessSubject: String?,
    val unmasteredCardsCount: Int,
    val completionRate: Float,
    val totalTasksCount: Int,
    val completedTasksCount: Int,
    val activeRecoveryPlan: ActiveRecoveryPlan? = null,
    val recoveredTasksCount: Int = 0
)

data class PlannedRecoveryTask(
    val task: StudyTask,
    val scheduledDayIndex: Int, // 0 = Today, 1 = Tomorrow, 2 = Day 3, etc.
    val scheduledDayLabel: String,
    val scheduledDateFormatted: String,
    val priorityReason: String
)

data class DailyRecoveryGroup(
    val dayIndex: Int,
    val dayLabel: String,
    val dateFormatted: String,
    val tasks: List<PlannedRecoveryTask>,
    val totalMinutes: Int
)

data class RecoveryPlan(
    val days: List<DailyRecoveryGroup>,
    val allPlannedTasks: List<PlannedRecoveryTask>,
    val totalTasksCount: Int,
    val totalStudyMinutes: Int,
    val daysPlanned: Int,
    val dailyPaceMinutes: Int
)

object FallingBehindDetector {

    /**
     * Determines whether an uncompleted task is overdue based on local device date/time.
     */
    fun isTaskOverdue(task: StudyTask): Boolean {
        if (task.isCompleted) return false

        // 1. Check textual or calendar due date against device's current local date
        val due = task.dueDate.trim()
        if (due.isNotBlank()) {
            if (DeviceTimeService.isDateBeforeToday(due)) {
                return true
            }
        }

        // 2. Check reminder epoch if scheduled in a past calendar day
        val epoch = task.reminderEpochMillis
        if (epoch != null) {
            val reminderLocalDate = java.time.Instant.ofEpochMilli(epoch)
                .atZone(DeviceTimeService.currentZoneId())
                .toLocalDate()
            if (reminderLocalDate.isBefore(DeviceTimeService.currentLocalDate())) {
                return true
            }
        }

        return false
    }

    /**
     * Analyzes local study data to diagnose whether a student is on track, slightly behind, or falling behind.
     */
    fun assessStudyStatus(
        tasks: List<StudyTask>,
        exams: List<ExamItem>,
        flashcards: List<FlashcardItem>,
        activePlan: ActiveRecoveryPlan?
    ): StudyStatusAssessment {
        val totalTasks = tasks.size
        val completedTasks = tasks.filter { it.isCompleted }
        val pendingTasks = tasks.filter { !it.isCompleted }
        val overdueTasks = tasks.filter { isTaskOverdue(it) }

        val completionRate = if (totalTasks > 0) completedTasks.size.toFloat() / totalTasks else 1f

        // Approaching exams (next 14 days)
        val approachingExam = exams
            .map { exam ->
                val days = DeviceTimeService.calculateDaysUntil(exam.examDate) ?: exam.daysLeft
                Pair(exam, days)
            }
            .filter { it.second in 0..14 }
            .minByOrNull { it.second }
            ?.first

        val examDaysLeft = approachingExam?.let {
            DeviceTimeService.calculateDaysUntil(it.examDate) ?: it.daysLeft
        }

        // Subject with lowest readiness / confidence
        val lowestReadinessSubject = exams
            .filter { it.confidenceLevel <= 3 || it.syllabusCoveredTopics < it.syllabusTotalTopics / 2 }
            .minByOrNull { it.confidenceLevel }
            ?.subject

        val unmasteredCardsCount = flashcards.count { !it.isMastered }

        // Upcoming important tasks
        val upcomingImportantTasks = pendingTasks
            .filter { !overdueTasks.contains(it) && (it.priority.equals("High", ignoreCase = true) || it.subject == approachingExam?.subject) }

        // Recovered tasks if an active recovery plan exists
        val recoveredCount = if (activePlan != null && activePlan.isActive) {
            tasks.count { it.id in activePlan.taskIds && it.isCompleted }
        } else {
            0
        }

        // Determine status
        val status: StudyPaceStatus
        val title: String
        val explanation: String

        if (overdueTasks.size >= 3 || (overdueTasks.isNotEmpty() && examDaysLeft != null && examDaysLeft <= 7) || (overdueTasks.size >= 2 && lowestReadinessSubject != null)) {
            status = StudyPaceStatus.FALLING_BEHIND
            title = "Falling Behind"

            explanation = when {
                overdueTasks.isNotEmpty() && approachingExam != null -> {
                    val countStr = if (overdueTasks.size == 1) "1 task is" else "${overdueTasks.size} tasks are"
                    val daysStr = if (examDaysLeft != null) " in ${examDaysLeft}d" else ""
                    "$countStr overdue and your ${approachingExam.subject} exam is approaching$daysStr."
                }
                overdueTasks.size >= 3 -> {
                    "${overdueTasks.size} tasks are overdue across multiple subjects."
                }
                else -> {
                    "${overdueTasks.size} tasks are overdue and readiness in ${lowestReadinessSubject ?: "core subjects"} is low."
                }
            }
        } else if (overdueTasks.isNotEmpty()) {
            status = StudyPaceStatus.SLIGHTLY_BEHIND
            title = "Slightly Behind"
            val taskWord = if (overdueTasks.size == 1) "task is" else "tasks are"
            explanation = "${overdueTasks.size} $taskWord overdue. A short catch-up session will get you back on track."
        } else if (pendingTasks.size >= 5) {
            status = StudyPaceStatus.SLIGHTLY_BEHIND
            title = "Slightly Behind"
            explanation = "You have ${pendingTasks.size} pending tasks. A quick focus session will keep your momentum strong."
        } else if (activePlan != null && activePlan.isActive) {
            status = StudyPaceStatus.ON_TRACK
            title = "Recovering"
            explanation = "Recovery plan active: $recoveredCount of ${activePlan.totalTasks} tasks completed. Keep up the momentum!"
        } else if (pendingTasks.isNotEmpty()) {
            status = StudyPaceStatus.ON_TRACK
            title = "On Track"
            explanation = "You have ${pendingTasks.size} upcoming study ${if (pendingTasks.size == 1) "task" else "tasks"}. Keep up the steady pace!"
        } else {
            status = StudyPaceStatus.ON_TRACK
            title = "On Track"
            explanation = "All caught up! You're on track with your study schedule."
        }

        return StudyStatusAssessment(
            status = status,
            title = title,
            explanation = explanation,
            overdueTasks = overdueTasks,
            pendingTasks = pendingTasks,
            upcomingImportantTasks = upcomingImportantTasks,
            approachingExam = approachingExam,
            examDaysLeft = examDaysLeft,
            lowestReadinessSubject = lowestReadinessSubject,
            unmasteredCardsCount = unmasteredCardsCount,
            completionRate = completionRate,
            totalTasksCount = totalTasks,
            completedTasksCount = completedTasks.size,
            activeRecoveryPlan = activePlan,
            recoveredTasksCount = recoveredCount
        )
    }

    /**
     * Generates a realistic recovery schedule using the student's existing tasks.
     * Prioritizes:
     * 1. Tasks closest to deadline or overdue
     * 2. Subjects with lower readiness or approaching exams
     * 3. Important revision tasks (High priority)
     * 4. Older overdue tasks
     * Spreads tasks across reasonable future days.
     */
    fun generateRecoveryPlan(
        tasks: List<StudyTask>,
        exams: List<ExamItem>,
        dailyPaceMinutes: Int = 90,
        daysAvailable: Int = 3
    ): RecoveryPlan {
        // Collect candidate tasks that need recovery
        val overdueTasks = tasks.filter { isTaskOverdue(it) }
        val pendingOtherTasks = tasks.filter { !it.isCompleted && !isTaskOverdue(it) }

        // Find approaching exam subjects
        val approachingExamSubjects = exams
            .filter { (DeviceTimeService.calculateDaysUntil(it.examDate) ?: it.daysLeft) <= 14 }
            .map { it.subject }
            .toSet()

        // Combine candidates: overdue tasks first, followed by remaining pending tasks
        val candidateTasks = (overdueTasks + pendingOtherTasks).distinctBy { it.id }

        // Score and sort candidates
        val sortedTasks = candidateTasks.sortedWith(
            compareByDescending<StudyTask> { task ->
                var score = 0
                if (isTaskOverdue(task)) score += 200
                if (approachingExamSubjects.contains(task.subject)) score += 50
                if (task.priority.equals("High", ignoreCase = true)) score += 30
                else if (task.priority.equals("Medium", ignoreCase = true)) score += 15
                score
            }.thenBy { it.dueDate }
        )

        // Distribute tasks across daysAvailable
        val effectiveDays = daysAvailable.coerceIn(2, 7)
        val targetDailyPace = dailyPaceMinutes.coerceIn(30, 240)

        val dayBuckets = MutableList(effectiveDays) { mutableListOf<PlannedRecoveryTask>() }
        val dayMinutes = IntArray(effectiveDays) { 0 }

        var currentDayIndex = 0

        for (task in sortedTasks) {
            val taskMinutes = task.estimatedMinutes.coerceAtLeast(15)

            // If adding to current day exceeds daily pace and current day already has tasks, advance day
            if (dayBuckets[currentDayIndex].isNotEmpty() && (dayMinutes[currentDayIndex] + taskMinutes > targetDailyPace)) {
                if (currentDayIndex < effectiveDays - 1) {
                    currentDayIndex++
                }
            }

            val scheduledDayIndex = currentDayIndex
            val dayLabel = when (scheduledDayIndex) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> {
                    val targetDate = DeviceTimeService.todayLocalDate().plusDays(scheduledDayIndex.toLong())
                    targetDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
                }
            }

            val dateFormatted = when (scheduledDayIndex) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> {
                    val targetDate = DeviceTimeService.todayLocalDate().plusDays(scheduledDayIndex.toLong())
                    targetDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                }
            }

            val priorityReason = when {
                isTaskOverdue(task) && approachingExamSubjects.contains(task.subject) -> "Overdue • ${task.subject} exam approaching"
                isTaskOverdue(task) -> "Overdue task"
                approachingExamSubjects.contains(task.subject) -> "High impact for upcoming exam"
                task.priority.equals("High", ignoreCase = true) -> "High priority goal"
                else -> "Study pace balance"
            }

            val planned = PlannedRecoveryTask(
                task = task,
                scheduledDayIndex = scheduledDayIndex,
                scheduledDayLabel = dayLabel,
                scheduledDateFormatted = dateFormatted,
                priorityReason = priorityReason
            )

            dayBuckets[scheduledDayIndex].add(planned)
            dayMinutes[scheduledDayIndex] += taskMinutes
        }

        // Build DailyRecoveryGroup list
        val dailyGroups = dayBuckets.mapIndexedNotNull { index, taskList ->
            if (taskList.isEmpty() && index >= 2) return@mapIndexedNotNull null
            val dayLabel = when (index) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> {
                    val targetDate = DeviceTimeService.todayLocalDate().plusDays(index.toLong())
                    targetDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
                }
            }
            val dateFormatted = when (index) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> {
                    val targetDate = DeviceTimeService.todayLocalDate().plusDays(index.toLong())
                    targetDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                }
            }
            DailyRecoveryGroup(
                dayIndex = index,
                dayLabel = dayLabel,
                dateFormatted = dateFormatted,
                tasks = taskList,
                totalMinutes = taskList.sumOf { it.task.estimatedMinutes }
            )
        }

        val allPlanned = dailyGroups.flatMap { it.tasks }
        val totalMinutes = allPlanned.sumOf { it.task.estimatedMinutes }

        return RecoveryPlan(
            days = dailyGroups,
            allPlannedTasks = allPlanned,
            totalTasksCount = allPlanned.size,
            totalStudyMinutes = totalMinutes,
            daysPlanned = dailyGroups.size,
            dailyPaceMinutes = targetDailyPace
        )
    }
}
