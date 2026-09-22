package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyTask
import com.example.data.model.StudiedTopic
import com.example.data.model.SubjectMark
import com.example.data.model.TimetableSlot
import com.example.data.model.WeakAreaTopic
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, id DESC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): StudyTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<StudyTask>)

    @Update
    suspend fun updateTask(task: StudyTask)

    @Delete
    suspend fun deleteTask(task: StudyTask)

    @Query("UPDATE study_tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun setTaskCompleted(id: Int, isCompleted: Boolean, completedAt: Long?)

    @Query("SELECT * FROM study_tasks WHERE isCompleted = 0 AND reminderEnabled = 1")
    suspend fun getActiveReminderTasks(): List<StudyTask>
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam_items ORDER BY daysLeft ASC")
    fun getAllExams(): Flow<List<ExamItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<ExamItem>)

    @Update
    suspend fun updateExam(exam: ExamItem)

    @Delete
    suspend fun deleteExam(exam: ExamItem)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_items ORDER BY isMastered ASC, id ASC")
    fun getAllFlashcards(): Flow<List<FlashcardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<FlashcardItem>)

    @Update
    suspend fun updateFlashcard(card: FlashcardItem)

    @Delete
    suspend fun deleteFlashcard(card: FlashcardItem)
}

@Dao
interface StudyNoteDao {
    @Query("SELECT * FROM study_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<StudyNote>>

    @Query("SELECT * FROM study_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): StudyNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<StudyNote>)

    @Update
    suspend fun updateNote(note: StudyNote)

    @Delete
    suspend fun deleteNote(note: StudyNote)

    @Query("UPDATE study_notes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean, updatedAt: Long)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY id ASC")
    fun getAllSlots(): Flow<List<TimetableSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlot): Long

    @Update
    suspend fun updateSlot(slot: TimetableSlot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<TimetableSlot>)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlot)
}

@Dao
interface WeakAreaDao {
    @Query("SELECT * FROM weak_area_topics ORDER BY isResolved ASC, id DESC")
    fun getAllWeakAreas(): Flow<List<WeakAreaTopic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeakArea(topic: WeakAreaTopic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<WeakAreaTopic>)

    @Update
    suspend fun updateWeakArea(topic: WeakAreaTopic)

    @Delete
    suspend fun deleteWeakArea(topic: WeakAreaTopic)
}

@Dao
interface StudentProfileDao {
    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<StudentProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: StudentProfile)

    @Query("UPDATE student_profile SET xpPoints = MAX(0, xpPoints + :points), level = MAX(1, (MAX(0, xpPoints + :points) / 350) + 1) WHERE id = 1")
    suspend fun addXp(points: Int)

    @Query("UPDATE student_profile SET xpPoints = MAX(0, xpPoints - :points), level = MAX(1, (MAX(0, xpPoints - :points) / 350) + 1) WHERE id = 1")
    suspend fun removeXp(points: Int)

    @Query("UPDATE student_profile SET streakDays = streakDays + 1 WHERE id = 1")
    suspend fun incrementStreak()

    @Query("UPDATE student_profile SET tasksCompletedCount = tasksCompletedCount + 1 WHERE id = 1")
    suspend fun incrementCompletedTasks()

    @Query("UPDATE student_profile SET tasksCompletedCount = MAX(0, tasksCompletedCount - 1) WHERE id = 1")
    suspend fun decrementCompletedTasks()
}

@Dao
interface SubjectMarkDao {
    @Query("SELECT * FROM subject_marks ORDER BY id ASC")
    fun getAllMarks(): Flow<List<SubjectMark>>

    @Query("SELECT * FROM subject_marks WHERE id = :id LIMIT 1")
    suspend fun getMarkById(id: Int): SubjectMark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMark(mark: SubjectMark): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(marks: List<SubjectMark>)

    @Update
    suspend fun updateMark(mark: SubjectMark)

    @Delete
    suspend fun deleteMark(mark: SubjectMark)

    @Query("DELETE FROM subject_marks")
    suspend fun clearAllMarks()
}

@Dao
interface StudiedTopicDao {
    @Query("SELECT * FROM studied_topics ORDER BY priorityScore DESC, scheduledTestEpochMillis ASC")
    fun getAllStudiedTopics(): Flow<List<StudiedTopic>>

    @Query("SELECT * FROM studied_topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: Int): StudiedTopic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: StudiedTopic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<StudiedTopic>)

    @Update
    suspend fun updateTopic(topic: StudiedTopic)

    @Delete
    suspend fun deleteTopic(topic: StudiedTopic)

    @Query("SELECT * FROM studied_topics WHERE memoryStrength = 'Needs Revision' ORDER BY priorityScore DESC")
    fun getTopicsNeedingRevision(): Flow<List<StudiedTopic>>

    @Query("UPDATE studied_topics SET memoryStrength = :strength, testCount = testCount + 1, lastTestedEpochMillis = :testedAt, priorityScore = :newPriority, scheduledTestEpochMillis = :nextTestAt WHERE id = :id")
    suspend fun recordTestResult(
        id: Int,
        strength: String,
        testedAt: Long,
        newPriority: Int,
        nextTestAt: Long
    )
}

