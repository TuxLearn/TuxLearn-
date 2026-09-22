package com.example.data.repository

import com.example.data.AppDatabase
import androidx.room.withTransaction
import com.example.data.InitialData
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.MemoryRecallOutcome
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyTask
import com.example.data.model.StudiedTopic
import com.example.data.model.SubjectMark
import com.example.data.model.TimetableSlot
import com.example.data.model.WeakAreaTopic
import com.example.notification.StudyReminderScheduler
import com.example.util.DeviceTimeService
import com.example.util.MemoryTestQuestionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudoraRepository(
    private val database: AppDatabase,
    private val reminderScheduler: StudyReminderScheduler? = null
) {
    private val taskDao = database.studyTaskDao()
    private val examDao = database.examDao()
    private val flashcardDao = database.flashcardDao()
    private val noteDao = database.studyNoteDao()
    private val timetableDao = database.timetableDao()
    private val weakAreaDao = database.weakAreaDao()
    private val profileDao = database.studentProfileDao()
    private val markDao = database.subjectMarkDao()
    private val studiedTopicDao = database.studiedTopicDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabaseIfEmpty()
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        val existingProfile = profileDao.getProfile().firstOrNull()
        if (existingProfile == null) {
            taskDao.insertAll(InitialData.sampleTasks)
            examDao.insertAll(InitialData.sampleExams)
            flashcardDao.insertAll(InitialData.sampleFlashcards)
            noteDao.insertAll(InitialData.sampleNotes)
            timetableDao.insertAll(InitialData.sampleTimetable)
            weakAreaDao.insertAll(InitialData.sampleWeakAreas)
            markDao.insertAll(InitialData.sampleMarks)
            profileDao.insertOrUpdate(InitialData.sampleProfile)
            studiedTopicDao.insertAll(InitialData.sampleStudiedTopics)
        } else {
            // Ensure sample studied topics exist if upgrading DB
            val existingTopics = studiedTopicDao.getAllStudiedTopics().firstOrNull()
            if (existingTopics.isNullOrEmpty()) {
                studiedTopicDao.insertAll(InitialData.sampleStudiedTopics)
            }
        }
    }

    // Tasks
    val allTasks: Flow<List<StudyTask>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Int): StudyTask? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: StudyTask): Long = withContext(Dispatchers.IO) {
        val id = database.withTransaction {
            val insertedId = taskDao.insertTask(task)
            if (task.isCompleted) {
                profileDao.addXp(task.xpReward)
                profileDao.incrementCompletedTasks()
            }
            insertedId
        }
        val savedTask = task.copy(id = id.toInt())
        if (savedTask.reminderEnabled && !savedTask.isCompleted) {
            reminderScheduler?.scheduleReminder(savedTask)
        }
        id
    }

    suspend fun updateTask(task: StudyTask) = withContext(Dispatchers.IO) {
        // Requirement 6: Cancel old reminder, schedule new reminder
        reminderScheduler?.cancelReminder(task.id)
        database.withTransaction {
            val oldTask = taskDao.getTaskById(task.id)
            if (oldTask != null && oldTask.isCompleted && task.isCompleted && oldTask.xpReward != task.xpReward) {
                val delta = task.xpReward - oldTask.xpReward
                if (delta > 0) {
                    profileDao.addXp(delta)
                } else if (delta < 0) {
                    profileDao.removeXp(-delta)
                }
            }
            taskDao.updateTask(task)
        }
        if (task.reminderEnabled && !task.isCompleted) {
            reminderScheduler?.scheduleReminder(task)
        }
    }

    suspend fun setTaskCompleted(taskId: Int, isCompleted: Boolean): Boolean = withContext(Dispatchers.IO) {
        val success = database.withTransaction {
            val currentTask = taskDao.getTaskById(taskId) ?: return@withTransaction false
            if (currentTask.isCompleted == isCompleted) {
                return@withTransaction false
            }
            val completedAt = if (isCompleted) System.currentTimeMillis() else null
            taskDao.setTaskCompleted(
                id = currentTask.id,
                isCompleted = isCompleted,
                completedAt = completedAt
            )
            if (isCompleted) {
                profileDao.addXp(currentTask.xpReward)
                profileDao.incrementCompletedTasks()
            } else {
                profileDao.removeXp(currentTask.xpReward)
                profileDao.decrementCompletedTasks()
            }
            true
        }
        if (success) {
            if (isCompleted) {
                // Requirement 4: Cancel pending reminder if task is marked completed
                reminderScheduler?.cancelReminder(taskId)
            } else {
                val currentTask = taskDao.getTaskById(taskId)
                if (currentTask != null && currentTask.reminderEnabled) {
                    reminderScheduler?.scheduleReminder(currentTask)
                }
            }
        }
        success
    }

    suspend fun toggleTaskCompleted(task: StudyTask): Boolean? = withContext(Dispatchers.IO) {
        val newStatus = database.withTransaction {
            val currentTask = taskDao.getTaskById(task.id) ?: return@withTransaction null
            val nextStatus = !currentTask.isCompleted
            val completedAt = if (nextStatus) System.currentTimeMillis() else null
            taskDao.setTaskCompleted(
                id = currentTask.id,
                isCompleted = nextStatus,
                completedAt = completedAt
            )
            if (nextStatus) {
                profileDao.addXp(currentTask.xpReward)
                profileDao.incrementCompletedTasks()
            } else {
                profileDao.removeXp(currentTask.xpReward)
                profileDao.decrementCompletedTasks()
            }
            nextStatus
        }
        if (newStatus == true) {
            // Requirement 4: Cancel pending reminder
            reminderScheduler?.cancelReminder(task.id)
        } else if (newStatus == false) {
            val currentTask = taskDao.getTaskById(task.id)
            if (currentTask != null && currentTask.reminderEnabled) {
                reminderScheduler?.scheduleReminder(currentTask)
            }
        }
        newStatus
    }

    suspend fun deleteTask(task: StudyTask) = withContext(Dispatchers.IO) {
        // Requirement 5: Cancel scheduled reminder
        reminderScheduler?.cancelReminder(task.id)
        database.withTransaction {
            val currentTask = taskDao.getTaskById(task.id) ?: task
            if (currentTask.isCompleted) {
                profileDao.removeXp(currentTask.xpReward)
                profileDao.decrementCompletedTasks()
            }
            taskDao.deleteTask(currentTask)
        }
    }

    // Exams
    val allExams: Flow<List<ExamItem>> = examDao.getAllExams()

    suspend fun insertExam(exam: ExamItem): Long = withContext(Dispatchers.IO) {
        examDao.insertExam(exam)
    }

    suspend fun updateExam(exam: ExamItem) = withContext(Dispatchers.IO) {
        examDao.updateExam(exam)
    }

    suspend fun deleteExam(exam: ExamItem) = withContext(Dispatchers.IO) {
        examDao.deleteExam(exam)
    }

    // Flashcards
    val allFlashcards: Flow<List<FlashcardItem>> = flashcardDao.getAllFlashcards()

    suspend fun insertFlashcard(card: FlashcardItem): Long = withContext(Dispatchers.IO) {
        flashcardDao.insertFlashcard(card)
    }

    suspend fun markFlashcardResult(card: FlashcardItem, mastered: Boolean) = withContext(Dispatchers.IO) {
        val updated = card.copy(
            isMastered = mastered,
            reviewCount = card.reviewCount + 1
        )
        flashcardDao.updateFlashcard(updated)
        if (mastered) {
            profileDao.addXp(15)
        }
    }

    suspend fun deleteFlashcard(card: FlashcardItem) = withContext(Dispatchers.IO) {
        flashcardDao.deleteFlashcard(card)
    }

    // Notes
    val allNotes: Flow<List<StudyNote>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Int): StudyNote? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: StudyNote): Long = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: StudyNote) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: StudyNote) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun toggleFavoriteNote(note: StudyNote) = withContext(Dispatchers.IO) {
        val newFav = !note.isFavorite
        noteDao.setFavorite(note.id, newFav, System.currentTimeMillis())
    }

    // Timetable
    val allTimetableSlots: Flow<List<TimetableSlot>> = timetableDao.getAllSlots()

    suspend fun insertTimetableSlot(slot: TimetableSlot): Long = withContext(Dispatchers.IO) {
        timetableDao.insertSlot(slot)
    }

    suspend fun updateTimetableSlot(slot: TimetableSlot) = withContext(Dispatchers.IO) {
        timetableDao.updateSlot(slot)
    }

    suspend fun deleteTimetableSlot(slot: TimetableSlot) = withContext(Dispatchers.IO) {
        timetableDao.deleteSlot(slot)
    }

    // Weak Areas
    val allWeakAreas: Flow<List<WeakAreaTopic>> = weakAreaDao.getAllWeakAreas()

    suspend fun insertWeakArea(topic: WeakAreaTopic): Long = withContext(Dispatchers.IO) {
        weakAreaDao.insertWeakArea(topic)
    }

    suspend fun toggleWeakAreaResolved(topic: WeakAreaTopic) = withContext(Dispatchers.IO) {
        val updated = topic.copy(isResolved = !topic.isResolved)
        weakAreaDao.updateWeakArea(updated)
        if (updated.isResolved) {
            profileDao.addXp(20)
        }
    }

    suspend fun deleteWeakArea(topic: WeakAreaTopic) = withContext(Dispatchers.IO) {
        weakAreaDao.deleteWeakArea(topic)
    }

    // Profile & Streak & XP
    val profile: Flow<StudentProfile?> = profileDao.getProfile()

    suspend fun updateProfile(profile: StudentProfile) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdate(profile)
    }

    suspend fun awardBonusXp(points: Int) = withContext(Dispatchers.IO) {
        profileDao.addXp(points)
    }

    // Subject Marks & Calculator
    val allSubjectMarks: Flow<List<SubjectMark>> = markDao.getAllMarks()

    suspend fun insertSubjectMark(mark: SubjectMark): Long = withContext(Dispatchers.IO) {
        markDao.insertMark(mark)
    }

    suspend fun updateSubjectMark(mark: SubjectMark) = withContext(Dispatchers.IO) {
        markDao.updateMark(mark)
    }

    suspend fun deleteSubjectMark(mark: SubjectMark) = withContext(Dispatchers.IO) {
        markDao.deleteMark(mark)
    }

    suspend fun clearAllSubjectMarks() = withContext(Dispatchers.IO) {
        markDao.clearAllMarks()
    }

    // --- Memory Test & Studied Topics System ---
    val allStudiedTopics: Flow<List<StudiedTopic>> = studiedTopicDao.getAllStudiedTopics()

    suspend fun getStudiedTopicById(id: Int): StudiedTopic? = withContext(Dispatchers.IO) {
        studiedTopicDao.getTopicById(id)
    }

    suspend fun recordStudiedTopic(
        subject: String,
        topic: String,
        durationMinutes: Int = 30
    ): StudiedTopic = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val formattedDate = try {
            val today = DeviceTimeService.todayLocalDate()
            val timeStr = DeviceTimeService.format12Hour(DeviceTimeService.currentLocalTime().hour, DeviceTimeService.currentLocalTime().minute)
            "Today, $timeStr"
        } catch (_: Exception) {
            "Today"
        }

        val qna = MemoryTestQuestionHelper.getQuestionForTopic(subject, topic)
        // Schedule test for later (e.g. 4 hours later) - active recall spaced repetition
        val scheduledTestEpoch = now + 4 * 3600 * 1000L

        val newTopic = StudiedTopic(
            subject = subject.ifBlank { "General" },
            topic = topic.trim(),
            studiedAtEpochMillis = now,
            studiedDateFormatted = formattedDate,
            studyDurationMinutes = durationMinutes.coerceAtLeast(5),
            memoryStrength = "Needs Revision",
            recallQuestion = qna.question,
            correctAnswer = qna.answer,
            explanation = qna.explanation,
            scheduledTestEpochMillis = scheduledTestEpoch,
            lastTestedEpochMillis = null,
            testCount = 0,
            forgotCount = 0,
            rememberedCount = 0,
            priorityScore = 15
        )

        val insertedId = studiedTopicDao.insertTopic(newTopic)
        newTopic.copy(id = insertedId.toInt())
    }

    suspend fun answerMemoryTest(topicId: Int, outcome: MemoryRecallOutcome): Int = withContext(Dispatchers.IO) {
        val topic = studiedTopicDao.getTopicById(topicId) ?: return@withContext 0
        val now = System.currentTimeMillis()
        val evaluation = MemoryTestQuestionHelper.evaluateRecallOutcome(topic, outcome, now)

        val updatedTopic = topic.copy(
            memoryStrength = evaluation.newMemoryStrength,
            priorityScore = evaluation.newPriorityScore,
            scheduledTestEpochMillis = evaluation.nextScheduledEpochMillis,
            lastTestedEpochMillis = now,
            testCount = topic.testCount + 1,
            forgotCount = evaluation.newForgotCount,
            rememberedCount = evaluation.newRememberedCount
        )

        database.withTransaction {
            studiedTopicDao.updateTopic(updatedTopic)
            profileDao.addXp(evaluation.xpEarned)
        }
        evaluation.xpEarned
    }

    suspend fun completeSurpriseRevision(topicId: Int, outcome: MemoryRecallOutcome): Int = withContext(Dispatchers.IO) {
        val topic = studiedTopicDao.getTopicById(topicId) ?: return@withContext 0
        val now = System.currentTimeMillis()
        val evaluation = MemoryTestQuestionHelper.evaluateRecallOutcome(topic, outcome, now)

        // Award bonus 35 XP specifically for completing the 60s Surprise Challenge
        val bonusXp = 35

        val updatedTopic = topic.copy(
            memoryStrength = evaluation.newMemoryStrength,
            priorityScore = evaluation.newPriorityScore,
            scheduledTestEpochMillis = evaluation.nextScheduledEpochMillis,
            lastTestedEpochMillis = now,
            testCount = topic.testCount + 1,
            forgotCount = evaluation.newForgotCount,
            rememberedCount = evaluation.newRememberedCount
        )

        database.withTransaction {
            studiedTopicDao.updateTopic(updatedTopic)
            profileDao.addXp(bonusXp)
        }
        bonusXp
    }

    suspend fun deleteStudiedTopic(topic: StudiedTopic) = withContext(Dispatchers.IO) {
        studiedTopicDao.deleteTopic(topic)
    }
}

