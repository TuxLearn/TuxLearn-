package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.InitialData
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyTask
import com.example.data.model.StudiedTopic
import com.example.data.model.MemoryRecallOutcome
import com.example.data.model.MemoryStrength
import com.example.data.model.SubjectMark
import com.example.data.model.TimetableSlot
import com.example.data.model.WeakAreaTopic
import com.example.data.repository.StudoraRepository
import com.example.notification.StudyReminderScheduler
import com.example.ui.navigation.AppScreen
import com.example.util.DeviceTimeService
import com.example.util.LocalDateTimeParts
import com.example.util.SmartReminderTimeHelper
import com.example.data.model.StudyExplanation
import com.example.data.model.PhotoSolveResult
import com.example.data.ActiveRecoveryPlan
import com.example.data.RecoveryPlanPreferences
import com.example.util.FallingBehindDetector
import com.example.util.RecoveryPlan
import com.example.util.StudyStatusAssessment
import com.example.util.ExplanationEngine
import com.example.util.PhotoSolveEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudoraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudoraRepository
    private val reminderScheduler: StudyReminderScheduler

    init {
        val database = AppDatabase.getDatabase(application)
        reminderScheduler = StudyReminderScheduler(application)
        repository = StudoraRepository(database, reminderScheduler)
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Master Reminder Preference State
    private val _masterReminderEnabled = MutableStateFlow(
        com.example.data.ReminderPreferences.isMasterEnabled(application)
    )
    val masterReminderEnabled: StateFlow<Boolean> = _masterReminderEnabled.asStateFlow()

    fun setMasterReminderEnabled(enabled: Boolean) {
        com.example.data.ReminderPreferences.setMasterEnabled(getApplication(), enabled)
        _masterReminderEnabled.value = enabled
        showFeedback(if (enabled) "Smart Reminders enabled" else "Smart Reminders paused")
    }

    // User notification banner / toast feedback
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    fun clearFeedback() {
        _userFeedback.value = null
    }

    private fun showFeedback(message: String) {
        _userFeedback.value = message
    }

    // Repository Flows
    val tasks: StateFlow<List<StudyTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamItem>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardItem>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<StudyNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timetable: StateFlow<List<TimetableSlot>> = repository.allTimetableSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weakAreas: StateFlow<List<WeakAreaTopic>> = repository.allWeakAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectMarks: StateFlow<List<SubjectMark>> = repository.allSubjectMarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studiedTopics: StateFlow<List<StudiedTopic>> = repository.allStudiedTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<StudentProfile> = repository.profile
        .map { it ?: InitialData.sampleProfile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InitialData.sampleProfile)

    // Surprise Challenge interactive state
    private val _surpriseChallengeSolved = MutableStateFlow(false)
    val surpriseChallengeSolved: StateFlow<Boolean> = _surpriseChallengeSolved.asStateFlow()

    private val _selectedChallengeAnswer = MutableStateFlow<Int?>(null)
    val selectedChallengeAnswer: StateFlow<Int?> = _selectedChallengeAnswer.asStateFlow()

    // Ask & Explain State
    private val _currentExplanation = MutableStateFlow<StudyExplanation?>(null)
    val currentExplanation: StateFlow<StudyExplanation?> = _currentExplanation.asStateFlow()

    private val _isExplaining = MutableStateFlow(false)
    val isExplaining: StateFlow<Boolean> = _isExplaining.asStateFlow()

    private val _explanationHistory = MutableStateFlow<List<StudyExplanation>>(emptyList())
    val explanationHistory: StateFlow<List<StudyExplanation>> = _explanationHistory.asStateFlow()

    // Photo Solve State
    private val _currentPhotoSolveResult = MutableStateFlow<PhotoSolveResult?>(null)
    val currentPhotoSolveResult: StateFlow<PhotoSolveResult?> = _currentPhotoSolveResult.asStateFlow()

    private val _selectedPhotoUri = MutableStateFlow<String?>(null)
    val selectedPhotoUri: StateFlow<String?> = _selectedPhotoUri.asStateFlow()

    private val _isPhotoSolving = MutableStateFlow(false)
    val isPhotoSolving: StateFlow<Boolean> = _isPhotoSolving.asStateFlow()

    private val _photoSolveScanningPhase = MutableStateFlow("")
    val photoSolveScanningPhase: StateFlow<String> = _photoSolveScanningPhase.asStateFlow()

    private val _photoSolveHistory = MutableStateFlow<List<PhotoSolveResult>>(emptyList())
    val photoSolveHistory: StateFlow<List<PhotoSolveResult>> = _photoSolveHistory.asStateFlow()

    // Active Recovery Plan State
    private val _activeRecoveryPlan = MutableStateFlow<ActiveRecoveryPlan?>(
        RecoveryPlanPreferences.getActivePlan(application)
    )
    val activeRecoveryPlan: StateFlow<ActiveRecoveryPlan?> = _activeRecoveryPlan.asStateFlow()

    // Real-time Study Status Assessment Flow
    val studyStatusAssessment: StateFlow<StudyStatusAssessment> = combine(
        tasks,
        exams,
        flashcards,
        _activeRecoveryPlan
    ) { currentTasks, currentExams, currentCards, currentPlan ->
        FallingBehindDetector.assessStudyStatus(
            tasks = currentTasks,
            exams = currentExams,
            flashcards = currentCards,
            activePlan = currentPlan
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FallingBehindDetector.assessStudyStatus(emptyList(), emptyList(), emptyList(), null)
    )

    fun applyRecoveryPlan(plan: RecoveryPlan) {
        viewModelScope.launch {
            val plannedTasks = plan.allPlannedTasks
            if (plannedTasks.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val taskIds = mutableListOf<Int>()

            plannedTasks.forEach { planned ->
                val task = planned.task
                taskIds.add(task.id)

                // Compute updated reminder epoch if reminder was enabled
                val newReminderEpoch = if (task.reminderEnabled && task.reminderEpochMillis != null) {
                    val existingParts = SmartReminderTimeHelper.fromEpochMillis(task.reminderEpochMillis)
                    val targetDate = DeviceTimeService.todayLocalDate().plusDays(planned.scheduledDayIndex.toLong())
                    SmartReminderTimeHelper.toLocalEpochMillis(
                        targetDate.year,
                        targetDate.monthValue - 1,
                        targetDate.dayOfMonth,
                        existingParts.hour,
                        existingParts.minute
                    )
                } else null

                val newReminderDate = if (newReminderEpoch != null) {
                    val parts = SmartReminderTimeHelper.fromEpochMillis(newReminderEpoch)
                    SmartReminderTimeHelper.formatShortDate(parts.year, parts.month0, parts.day)
                } else task.reminderDateFormatted

                val updatedTask = task.copy(
                    dueDate = planned.scheduledDayLabel,
                    reminderEpochMillis = newReminderEpoch ?: task.reminderEpochMillis,
                    reminderDateFormatted = newReminderDate
                )
                repository.updateTask(updatedTask)
            }

            val newActivePlan = ActiveRecoveryPlan(
                isActive = true,
                taskIds = taskIds,
                totalTasks = taskIds.size,
                daysPlanned = plan.daysPlanned,
                totalStudyMinutes = plan.totalStudyMinutes,
                appliedEpoch = now
            )

            RecoveryPlanPreferences.setActivePlan(getApplication(), newActivePlan)
            _activeRecoveryPlan.value = newActivePlan
            showFeedback("Recovery plan applied! ${taskIds.size} tasks rescheduled across ${plan.daysPlanned} days.")
        }
    }

    fun cancelActiveRecoveryPlan() {
        RecoveryPlanPreferences.clearActivePlan(getApplication())
        _activeRecoveryPlan.value = null
        showFeedback("Recovery plan cleared.")
    }

    // Tasks Actions
    fun toggleTask(task: StudyTask) {
        viewModelScope.launch {
            val newStatus = repository.toggleTaskCompleted(task)
            if (newStatus == true) {
                showFeedback("🎉 Great job! +${task.xpReward} XP earned")
            } else if (newStatus == false) {
                showFeedback("Task marked incomplete (-${task.xpReward} XP)")
            }
        }
    }

    fun addTask(
        title: String,
        subject: String,
        dueDate: String,
        minutes: Int,
        priority: String,
        reminderEnabled: Boolean = false,
        reminderEpochMillis: Long? = null,
        reminderDateFormatted: String = "",
        reminderTimeFormatted: String = "",
        reminderRepeat: String = "Once",
        reminderCategory: String = "Study session",
        xpReward: Int? = null
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedSubject = subject.trim()
            val rewardXp = xpReward ?: when (priority) {
                "High" -> 35
                "Medium" -> 25
                else -> 20
            }
            val existing = tasks.value.firstOrNull {
                it.title.equals(trimmedTitle, ignoreCase = true) &&
                it.subject.equals(trimmedSubject, ignoreCase = true) &&
                !it.isCompleted
            }
            if (existing != null) {
                val updated = existing.copy(
                    dueDate = dueDate,
                    estimatedMinutes = minutes,
                    priority = priority,
                    xpReward = rewardXp,
                    reminderEnabled = reminderEnabled,
                    reminderEpochMillis = if (reminderEnabled) reminderEpochMillis else existing.reminderEpochMillis,
                    reminderDateFormatted = if (reminderEnabled) reminderDateFormatted else existing.reminderDateFormatted,
                    reminderTimeFormatted = if (reminderTimeFormatted.isNotBlank()) reminderTimeFormatted else existing.reminderTimeFormatted,
                    reminderRepeat = reminderRepeat,
                    reminderCategory = reminderCategory
                )
                repository.updateTask(updated)
                showFeedback("Updated existing task: ${existing.title}")
                return@launch
            }

            val newTask = StudyTask(
                title = trimmedTitle,
                subject = trimmedSubject,
                dueDate = dueDate,
                estimatedMinutes = minutes,
                priority = priority,
                reminderEnabled = reminderEnabled,
                reminderEpochMillis = if (reminderEnabled) reminderEpochMillis else null,
                reminderDateFormatted = if (reminderEnabled) reminderDateFormatted else "",
                reminderTimeFormatted = reminderTimeFormatted,
                reminderRepeat = reminderRepeat,
                reminderCategory = reminderCategory,
                xpReward = rewardXp
            )
            repository.insertTask(newTask)
            if (reminderEnabled && reminderEpochMillis != null) {
                showFeedback("Reminder scheduled for $reminderTimeFormatted ($reminderCategory)")
            } else {
                showFeedback("Task added to Study Planner (+$rewardXp XP)")
            }
        }
    }

    fun updateReminder(
        task: StudyTask,
        title: String,
        subject: String,
        dateStr: String,
        timeStr: String,
        epochMillis: Long,
        repeat: String,
        category: String,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            val updated = task.copy(
                title = title.trim(),
                subject = subject.trim(),
                reminderDateFormatted = dateStr,
                reminderTimeFormatted = timeStr,
                reminderEpochMillis = epochMillis,
                reminderRepeat = repeat,
                reminderCategory = category,
                reminderEnabled = enabled
            )
            repository.updateTask(updated)
            showFeedback("Reminder updated: \"${title.trim()}\"")
        }
    }

    fun toggleReminderEnabled(task: StudyTask) {
        viewModelScope.launch {
            val nextEnabled = !task.reminderEnabled
            val now = System.currentTimeMillis()
            // If turning on and time is in past or null, set default future time (30 mins from now)
            val nextMillis = if (nextEnabled && (task.reminderEpochMillis == null || task.reminderEpochMillis <= now)) {
                val futureParts = SmartReminderTimeHelper.getInitialFutureReminderTime()
                SmartReminderTimeHelper.toLocalEpochMillis(
                    futureParts.year,
                    futureParts.month0,
                    futureParts.day,
                    futureParts.hour,
                    futureParts.minute
                )
            } else {
                task.reminderEpochMillis
            }

            val (dateStr, timeStr) = if (nextMillis != null) {
                val parts = SmartReminderTimeHelper.fromEpochMillis(nextMillis)
                Pair(
                    SmartReminderTimeHelper.formatShortDate(parts.year, parts.month0, parts.day),
                    SmartReminderTimeHelper.format12Hour(parts.hour, parts.minute)
                )
            } else {
                Pair("", "")
            }

            val updated = task.copy(
                reminderEnabled = nextEnabled,
                reminderEpochMillis = if (nextEnabled) nextMillis else task.reminderEpochMillis,
                reminderDateFormatted = if (nextEnabled && task.reminderDateFormatted.isBlank()) dateStr else task.reminderDateFormatted,
                reminderTimeFormatted = if (nextEnabled && task.reminderTimeFormatted.isBlank()) timeStr else task.reminderTimeFormatted
            )
            repository.updateTask(updated)
            if (nextEnabled) {
                showFeedback("⏰ Reminder enabled for ${updated.reminderTimeFormatted.ifBlank { "scheduled time" }}")
            } else {
                showFeedback("🔕 Reminder disabled")
            }
        }
    }

    fun snoozeReminder(task: StudyTask, minutes: Int = 15) {
        viewModelScope.launch {
            val newMillis = System.currentTimeMillis() + minutes * 60 * 1000L
            val parts = SmartReminderTimeHelper.fromEpochMillis(newMillis)
            val dateStr = SmartReminderTimeHelper.formatShortDate(parts.year, parts.month0, parts.day)
            val timeStr = SmartReminderTimeHelper.format12Hour(parts.hour, parts.minute)

            val updated = task.copy(
                reminderEnabled = true,
                reminderEpochMillis = newMillis,
                reminderDateFormatted = dateStr,
                reminderTimeFormatted = timeStr
            )
            repository.updateTask(updated)
            showFeedback("⏰ Reminding in $minutes mins ($timeStr)")
        }
    }

    fun rescheduleReminder(
        task: StudyTask,
        epochMillis: Long,
        dateFormatted: String,
        timeFormatted: String,
        repeat: String = task.reminderRepeat
    ) {
        viewModelScope.launch {
            val updated = task.copy(
                reminderEnabled = true,
                reminderEpochMillis = epochMillis,
                reminderDateFormatted = dateFormatted,
                reminderTimeFormatted = timeFormatted,
                reminderRepeat = repeat
            )
            repository.updateTask(updated)
            showFeedback("⏰ Rescheduled for $dateFormatted at $timeFormatted")
        }
    }

    fun sendTestNotification(task: StudyTask? = null) {
        viewModelScope.launch {
            val posted = reminderScheduler.sendTestNotification(task)
            if (posted) {
                showFeedback("Test notification sent. Check your Android notifications.")
            } else {
                showFeedback("Notification permission is required to display alerts. Please grant permission.")
            }
        }
    }

    fun scheduleTestNotificationIn10Seconds() {
        viewModelScope.launch {
            val scheduled = reminderScheduler.scheduleTestNotificationIn10Seconds()
            if (scheduled) {
                showFeedback("Test notification scheduled for 10 seconds. Check your notifications.")
            } else {
                showFeedback("Notification permission is required to display alerts. Please grant permission.")
            }
        }
    }

    fun updateTask(task: StudyTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            if (task.reminderEnabled && task.reminderEpochMillis != null) {
                showFeedback("Task updated & reminder set for ${task.reminderTimeFormatted}")
            } else {
                showFeedback("Task updated")
            }
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            showFeedback("Task removed")
        }
    }

    // Surprise Challenge
    fun solveSurpriseChallenge(optionIndex: Int, isCorrect: Boolean) {
        _selectedChallengeAnswer.value = optionIndex
        if (isCorrect && !_surpriseChallengeSolved.value) {
            _surpriseChallengeSolved.value = true
            viewModelScope.launch {
                repository.awardBonusXp(30)
                showFeedback("⚡ Perfect Recall! +30 XP earned")
            }
        }
    }

    fun resetSurpriseChallenge() {
        _surpriseChallengeSolved.value = false
        _selectedChallengeAnswer.value = null
    }

    // Flashcards Actions
    fun markFlashcard(card: FlashcardItem, mastered: Boolean) {
        viewModelScope.launch {
            repository.markFlashcardResult(card, mastered)
            if (mastered) {
                showFeedback("✨ Mastered! +15 XP")
            }
        }
    }

    fun addFlashcard(subject: String, question: String, answer: String, hint: String) {
        viewModelScope.launch {
            repository.insertFlashcard(
                FlashcardItem(
                    subject = subject,
                    question = question,
                    answer = answer,
                    hint = hint
                )
            )
            showFeedback("Flashcard created")
        }
    }

    fun deleteFlashcard(card: FlashcardItem) {
        viewModelScope.launch {
            repository.deleteFlashcard(card)
            showFeedback("Flashcard removed")
        }
    }

    // Memory Test Actions
    fun markTopicStudied(
        subject: String,
        topic: String,
        durationMinutes: Int = 30
    ) {
        viewModelScope.launch {
            val created = repository.recordStudiedTopic(subject, topic, durationMinutes)
            showFeedback("Topic \"${created.topic}\" saved! Memory test scheduled for later recall 🧠")
        }
    }

    fun answerMemoryTest(topicId: Int, outcome: MemoryRecallOutcome) {
        viewModelScope.launch {
            val xp = repository.answerMemoryTest(topicId, outcome)
            val label = when (outcome) {
                MemoryRecallOutcome.REMEMBERED -> "Strong ✨"
                MemoryRecallOutcome.PARTIALLY_REMEMBERED -> "Medium ⚡"
                MemoryRecallOutcome.FORGOT -> "Needs Revision (queued for high frequency) ⚠️"
            }
            showFeedback("Memory marked as $label! +$xp XP")
        }
    }

    fun completeSurpriseRevision(topicId: Int, outcome: MemoryRecallOutcome) {
        viewModelScope.launch {
            val xp = repository.completeSurpriseRevision(topicId, outcome)
            showFeedback("Surprise Recall Challenge completed! +$xp XP 🎉")
        }
    }

    fun deleteStudiedTopic(topic: StudiedTopic) {
        viewModelScope.launch {
            repository.deleteStudiedTopic(topic)
            showFeedback("Studied topic removed")
        }
    }

    // Notes Actions
    fun addNote(
        title: String,
        subject: String,
        chapter: String = "",
        content: String,
        tags: String = "",
        isFavorite: Boolean = false
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.insertNote(
                StudyNote(
                    title = title,
                    subject = subject,
                    chapter = chapter,
                    content = content,
                    tags = tags,
                    isFavorite = isFavorite,
                    createdAt = now,
                    updatedAt = now
                )
            )
            showFeedback("Note created")
        }
    }

    fun addNote(title: String, subject: String, content: String, tags: String) {
        addNote(title = title, subject = subject, chapter = "", content = content, tags = tags, isFavorite = false)
    }

    fun updateNote(note: StudyNote) {
        viewModelScope.launch {
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            showFeedback("Note updated")
        }
    }

    fun toggleFavoriteNote(note: StudyNote) {
        viewModelScope.launch {
            repository.toggleFavoriteNote(note)
            showFeedback(if (!note.isFavorite) "Added to Favorites ⭐" else "Removed from Favorites")
        }
    }

    fun deleteNote(note: StudyNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            showFeedback("Note deleted")
        }
    }

    // Timetable Actions
    fun addTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            val id = repository.insertTimetableSlot(slot)
            if (slot.reminderEnabled) {
                scheduleTimetableReminder(slot.copy(id = id.toInt()))
            }
            showFeedback("Class scheduled for ${slot.dayOfWeek}")
        }
    }

    fun addTimetableSlot(
        dayOfWeek: String,
        startTime: String,
        endTime: String,
        subject: String,
        roomOrTeacher: String,
        isStudySession: Boolean
    ) {
        addTimetableSlot(
            TimetableSlot(
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                subject = subject,
                roomOrTeacher = roomOrTeacher,
                isStudySession = isStudySession
            )
        )
    }

    fun updateTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repository.updateTimetableSlot(slot)
            if (slot.reminderEnabled) {
                scheduleTimetableReminder(slot)
            }
            showFeedback("Class updated")
        }
    }

    fun deleteTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            repository.deleteTimetableSlot(slot)
            showFeedback("Class removed from timetable")
        }
    }

    private fun scheduleTimetableReminder(slot: TimetableSlot) {
        viewModelScope.launch {
            try {
                val targetDayOfWeek = when (slot.dayOfWeek.trim().lowercase()) {
                    "mon", "monday" -> java.time.DayOfWeek.MONDAY
                    "tue", "tuesday" -> java.time.DayOfWeek.TUESDAY
                    "wed", "wednesday" -> java.time.DayOfWeek.WEDNESDAY
                    "thu", "thursday" -> java.time.DayOfWeek.THURSDAY
                    "fri", "friday" -> java.time.DayOfWeek.FRIDAY
                    "sat", "saturday" -> java.time.DayOfWeek.SATURDAY
                    "sun", "sunday" -> java.time.DayOfWeek.SUNDAY
                    else -> java.time.DayOfWeek.MONDAY
                }

                val startMins = DeviceTimeService.parseTimeToMinutes(slot.startTime)
                val endMins = DeviceTimeService.parseTimeToMinutes(slot.endTime)
                val durationMins = (endMins - startMins).coerceAtLeast(30)

                val hour = (startMins / 60).coerceIn(0, 23)
                val min = (startMins % 60).coerceIn(0, 59)
                val localTime = java.time.LocalTime.of(hour, min)

                val now = DeviceTimeService.nowZoned()
                var targetDate = now.toLocalDate()

                val currentDOW = targetDate.dayOfWeek
                val daysToAdd = (targetDayOfWeek.value - currentDOW.value + 7) % 7
                targetDate = targetDate.plusDays(daysToAdd.toLong())

                var targetZoned = java.time.ZonedDateTime.of(targetDate, localTime, DeviceTimeService.currentZoneId())
                if (!targetZoned.isAfter(now)) {
                    targetZoned = targetZoned.plusWeeks(1)
                }

                val epochMillis = targetZoned.toInstant().toEpochMilli()
                val dateFormatted = targetZoned.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.getDefault()))
                val timeFormatted = targetZoned.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault()))

                val task = StudyTask(
                    title = "Class: ${slot.subject}",
                    subject = slot.subject,
                    dueDate = dateFormatted,
                    estimatedMinutes = durationMins,
                    priority = "High",
                    reminderEnabled = true,
                    reminderEpochMillis = epochMillis,
                    reminderDateFormatted = dateFormatted,
                    reminderTimeFormatted = timeFormatted,
                    reminderRepeat = "Weekly",
                    reminderCategory = "Study session"
                )
                repository.insertTask(task)
            } catch (e: Exception) {
                android.util.Log.e("StudoraViewModel", "Failed to schedule timetable reminder", e)
            }
        }
    }

    // Weak Areas & Exam Readiness
    fun addWeakArea(subject: String, topicName: String, priority: String) {
        viewModelScope.launch {
            repository.insertWeakArea(
                WeakAreaTopic(
                    subject = subject,
                    topicName = topicName,
                    priority = priority
                )
            )
            showFeedback("Topic added to focus list")
        }
    }

    fun toggleWeakArea(topic: WeakAreaTopic) {
        viewModelScope.launch {
            repository.toggleWeakAreaResolved(topic)
            if (!topic.isResolved) {
                showFeedback("🎯 Topic conquered! +20 XP")
            }
        }
    }

    fun deleteWeakArea(topic: WeakAreaTopic) {
        viewModelScope.launch {
            repository.deleteWeakArea(topic)
        }
    }

    fun addExam(subject: String, name: String, date: String, daysLeft: Int, totalTopics: Int, coveredTopics: Int, confidence: Int, targetMarks: Int) {
        viewModelScope.launch {
            repository.insertExam(
                ExamItem(
                    subject = subject,
                    examName = name,
                    examDate = date,
                    daysLeft = daysLeft,
                    syllabusTotalTopics = totalTopics,
                    syllabusCoveredTopics = coveredTopics,
                    confidenceLevel = confidence,
                    targetMarks = targetMarks
                )
            )
            showFeedback("Exam scheduled")
        }
    }

    fun updateExamProgress(exam: ExamItem, coveredTopics: Int, confidence: Int) {
        viewModelScope.launch {
            repository.updateExam(
                exam.copy(
                    syllabusCoveredTopics = coveredTopics,
                    confidenceLevel = confidence
                )
            )
            showFeedback("Exam progress updated")
        }
    }

    fun deleteExam(exam: ExamItem) {
        viewModelScope.launch {
            repository.deleteExam(exam)
            showFeedback("Exam removed")
        }
    }

    fun updateProfileInfo(name: String, grade: String, targetGpa: String) {
        viewModelScope.launch {
            val current = profile.value
            repository.updateProfile(
                current.copy(
                    name = name,
                    grade = grade,
                    targetGpa = targetGpa
                )
            )
            showFeedback("Profile updated")
        }
    }

    // Marks & Percentage Calculator Actions
    fun addSubjectMark(subjectName: String, maxMarks: Double, obtainedMarks: Double) {
        viewModelScope.launch {
            val mark = SubjectMark(
                subjectName = subjectName.trim(),
                maxMarks = maxMarks,
                obtainedMarks = obtainedMarks
            )
            repository.insertSubjectMark(mark)
            showFeedback("Added ${mark.subjectName} marks")
        }
    }

    fun updateSubjectMark(mark: SubjectMark) {
        viewModelScope.launch {
            repository.updateSubjectMark(mark)
            showFeedback("Updated ${mark.subjectName}")
        }
    }

    fun deleteSubjectMark(mark: SubjectMark) {
        viewModelScope.launch {
            repository.deleteSubjectMark(mark)
            showFeedback("Removed ${mark.subjectName}")
        }
    }

    fun clearAllSubjectMarks() {
        viewModelScope.launch {
            repository.clearAllSubjectMarks()
            showFeedback("Calculator reset successfully")
        }
    }

    // Ask & Explain Actions
    fun explainQuestion(question: String, subject: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) {
            showFeedback("Please type a question to explain")
            return
        }
        viewModelScope.launch {
            _isExplaining.value = true
            delay(350) // Pleasant transition for user perception
            val result = ExplanationEngine.explainQuestion(trimmed, subject)
            _currentExplanation.value = result
            _isExplaining.value = false
            _explanationHistory.update { existing ->
                listOf(result) + existing.filter { it.question != result.question }.take(9)
            }
            showFeedback("Explanation ready!")
        }
    }

    fun clearExplanation() {
        _currentExplanation.value = null
    }

    fun saveExplanationToNotes(explanation: StudyExplanation) {
        val noteContent = buildString {
            appendLine("### 🎯 Direct Answer")
            appendLine(explanation.directAnswer)
            appendLine()
            appendLine("### 💡 Easy Explanation")
            appendLine(explanation.easyExplanation)
            appendLine()
            appendLine("### 🌟 Example")
            appendLine(explanation.example)
            appendLine()
            appendLine("### ⚠️ Important Exam Point")
            appendLine(explanation.examPoint)
            appendLine()
            appendLine("### ⚡ Quick Recall Question")
            appendLine("Q: ${explanation.quickRecallQuestion}")
            appendLine("A: ${explanation.quickRecallAnswer}")
        }
        addNote(
            title = explanation.question,
            subject = explanation.subject,
            chapter = "Ask & Explain",
            content = noteContent,
            tags = "AskExplain, ${explanation.subject}",
            isFavorite = true
        )
        showFeedback("Saved to Notes!")
    }

    fun addExplanationToRevision(explanation: StudyExplanation) {
        addFlashcard(
            subject = explanation.subject,
            question = explanation.quickRecallQuestion.ifBlank { explanation.question },
            answer = "${explanation.quickRecallAnswer}\n\nDirect Answer: ${explanation.directAnswer}",
            hint = explanation.example
        )
        showFeedback("Added to Revision!")
    }

    // Photo Solve Actions
    fun setPhotoUri(uriString: String?) {
        _selectedPhotoUri.value = uriString
    }

    fun solvePhoto(
        uriString: String? = null,
        rawQuestion: String? = null,
        sampleId: String? = null
    ) {
        viewModelScope.launch {
            _isPhotoSolving.value = true
            _photoSolveScanningPhase.value = "Scanning image & detecting question..."
            delay(280)

            _photoSolveScanningPhase.value = "Extracting text and mathematical notation..."
            delay(280)

            _photoSolveScanningPhase.value = "Identifying subject, topic & analyzing solution..."
            delay(320)

            val result: PhotoSolveResult = if (sampleId != null) {
                PhotoSolveEngine.solveSamplePhoto(sampleId)
            } else if (!rawQuestion.isNullOrBlank()) {
                PhotoSolveEngine.solveExtractedQuestion(
                    rawQuestion = rawQuestion,
                    imageUriString = uriString
                )
            } else if (uriString != null) {
                // Real photo picked from gallery: OCR simulation with subject identification
                PhotoSolveEngine.solveExtractedQuestion(
                    rawQuestion = "Solve the question captured in photo: Evaluate the integral or calculate the velocity from the problem.",
                    imageUriString = uriString
                )
            } else {
                PhotoSolveEngine.solveExtractedQuestion(
                    rawQuestion = "",
                    imageUriString = null
                )
            }

            _currentPhotoSolveResult.value = result
            _isPhotoSolving.value = false
            _photoSolveScanningPhase.value = ""

            if (result.isReadable) {
                _photoSolveHistory.update { existing ->
                    listOf(result) + existing.filter { it.id != result.id }.take(9)
                }
                showFeedback("Photo solved! ${result.identifiedSubject} • ${result.identifiedTopic}")
            } else {
                showFeedback("Photo unclear: ${result.unreadableReason?.take(40)}...")
            }
        }
    }

    fun reSolveEditedQuestion(newQuestionText: String) {
        val current = _currentPhotoSolveResult.value
        val imageUri = current?.imageUriString ?: _selectedPhotoUri.value
        solvePhoto(uriString = imageUri, rawQuestion = newQuestionText, sampleId = null)
    }

    fun clearPhotoSolve() {
        _currentPhotoSolveResult.value = null
        _selectedPhotoUri.value = null
        _photoSolveScanningPhase.value = ""
    }

    fun savePhotoSolutionToNotes(result: PhotoSolveResult) {
        val noteContent = buildString {
            appendLine("### ❓ Extracted Question")
            appendLine(result.extractedQuestion)
            appendLine()
            appendLine("### 🏷️ Subject & Topic")
            appendLine("${result.identifiedSubject} — ${result.identifiedTopic}")
            appendLine()
            appendLine("### 🎯 Direct Answer")
            appendLine(result.directAnswer)
            appendLine()
            appendLine("### 📋 Step-by-Step Explanation")
            result.stepByStepExplanation.forEach { step ->
                appendLine("**Step ${step.stepNumber}: ${step.title}**")
                if (!step.formulaOrWork.isNullOrBlank()) {
                    appendLine("Formula/Work: ${step.formulaOrWork}")
                }
                appendLine(step.explanation)
                appendLine()
            }
            appendLine("### 💡 Easy Explanation")
            appendLine(result.easyExplanation)
            appendLine()
            appendLine("### ⚠️ Important Exam Point")
            appendLine(result.examPoint)
        }
        addNote(
            title = result.extractedQuestion.take(45),
            subject = result.identifiedSubject,
            chapter = "Photo Solve",
            content = noteContent,
            tags = "PhotoSolve, ${result.identifiedSubject}",
            isFavorite = true
        )
        showFeedback("Photo solution saved to Notes!")
    }

    fun addPhotoSolutionToRevision(result: PhotoSolveResult) {
        val stepsSummary = result.stepByStepExplanation.joinToString("\n") {
            "• Step ${it.stepNumber}: ${it.title}"
        }
        val flashcardAnswer = buildString {
            appendLine("🎯 Direct Answer: ${result.directAnswer}")
            if (stepsSummary.isNotBlank()) {
                appendLine()
                appendLine("Key Working Steps:")
                appendLine(stepsSummary)
            }
            appendLine()
            appendLine("Exam Tip: ${result.examPoint}")
        }

        addFlashcard(
            subject = result.identifiedSubject,
            question = result.extractedQuestion,
            answer = flashcardAnswer.trim(),
            hint = result.easyExplanation
        )
        showFeedback("Added to Revision Deck!")
    }
}

