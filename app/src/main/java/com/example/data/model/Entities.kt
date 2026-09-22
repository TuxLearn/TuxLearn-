package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val dueDate: String,
    val estimatedMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: String = "Medium", // "High", "Medium", "Low"
    val xpReward: Int = 25,
    val reminderEnabled: Boolean = false,
    val reminderEpochMillis: Long? = null,
    val reminderDateFormatted: String = "",
    val reminderTimeFormatted: String = "",
    val reminderRepeat: String = "Once", // "Once", "Daily", "Weekly"
    val reminderCategory: String = "Study session" // "Study session", "Homework", "Revision", "Exam preparation"
)

@Entity(tableName = "exam_items")
data class ExamItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val examName: String,
    val examDate: String,
    val daysLeft: Int,
    val syllabusTotalTopics: Int = 10,
    val syllabusCoveredTopics: Int = 7,
    val confidenceLevel: Int = 4, // 1 to 5
    val targetMarks: Int = 90
)

@Entity(tableName = "flashcard_items")
data class FlashcardItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val question: String,
    val answer: String,
    val hint: String = "",
    val isMastered: Boolean = false,
    val reviewCount: Int = 0
)

@Entity(tableName = "study_notes")
data class StudyNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val chapter: String = "",
    val content: String,
    val tags: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timetable_slots")
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String, // Mon, Tue, Wed, Thu, Fri, Sat, Sun or Monday, etc.
    val startTime: String,
    val endTime: String,
    val subject: String,
    val roomOrTeacher: String = "",
    val isStudySession: Boolean = false,
    val teacherName: String = "",
    val roomOrClass: String = "",
    val colorCategory: String = "Indigo",
    val note: String = "",
    val reminderEnabled: Boolean = false
)

@Entity(tableName = "weak_area_topics")
data class WeakAreaTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val topicName: String,
    val isResolved: Boolean = false,
    val priority: String = "High"
)

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Alex Rivera",
    val grade: String = "High School / College Prep",
    val streakDays: Int = 7,
    val xpPoints: Int = 850,
    val level: Int = 3,
    val targetGpa: String = "3.9 / 95%",
    val totalStudyHours: Double = 42.5,
    val tasksCompletedCount: Int = 24
)

@Entity(tableName = "subject_marks")
data class SubjectMark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val maxMarks: Double,
    val obtainedMarks: Double,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MemoryStrength(val label: String) {
    STRONG("Strong"),
    MEDIUM("Medium"),
    NEEDS_REVISION("Needs Revision")
}

enum class MemoryRecallOutcome(val label: String) {
    REMEMBERED("I Remembered"),
    PARTIALLY_REMEMBERED("Partially Remembered"),
    FORGOT("I Forgot")
}

@Entity(tableName = "studied_topics")
data class StudiedTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val topic: String,
    val studiedAtEpochMillis: Long = System.currentTimeMillis(),
    val studiedDateFormatted: String = "",
    val studyDurationMinutes: Int = 30,
    val memoryStrength: String = "Needs Revision", // "Strong", "Medium", "Needs Revision"
    val recallQuestion: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val scheduledTestEpochMillis: Long = System.currentTimeMillis() + 4 * 3600 * 1000L,
    val lastTestedEpochMillis: Long? = null,
    val testCount: Int = 0,
    val forgotCount: Int = 0,
    val rememberedCount: Int = 0,
    val priorityScore: Int = 10 // higher score = appears earlier/more frequently in future tests
)
