package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ExamDao
import com.example.data.dao.FlashcardDao
import com.example.data.dao.StudentProfileDao
import com.example.data.dao.StudyNoteDao
import com.example.data.dao.StudyTaskDao
import com.example.data.dao.StudiedTopicDao
import com.example.data.dao.SubjectMarkDao
import com.example.data.dao.TimetableDao
import com.example.data.dao.WeakAreaDao
import com.example.data.model.ExamItem
import com.example.data.model.FlashcardItem
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyTask
import com.example.data.model.StudiedTopic
import com.example.data.model.SubjectMark
import com.example.data.model.TimetableSlot
import com.example.data.model.WeakAreaTopic

@Database(
    entities = [
        StudyTask::class,
        ExamItem::class,
        FlashcardItem::class,
        StudyNote::class,
        TimetableSlot::class,
        WeakAreaTopic::class,
        StudentProfile::class,
        SubjectMark::class,
        StudiedTopic::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun examDao(): ExamDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun studyNoteDao(): StudyNoteDao
    abstract fun timetableDao(): TimetableDao
    abstract fun weakAreaDao(): WeakAreaDao
    abstract fun studentProfileDao(): StudentProfileDao
    abstract fun subjectMarkDao(): SubjectMarkDao
    abstract fun studiedTopicDao(): StudiedTopicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studora_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
