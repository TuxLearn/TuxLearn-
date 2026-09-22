package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialData
import com.example.data.model.StudyNote
import com.example.data.repository.StudoraRepository
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
class NotesStudyMaterialsTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: StudoraRepository

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        database.studentProfileDao().insertOrUpdate(InitialData.sampleProfile)
        database.studyNoteDao().insertAll(InitialData.sampleNotes)
        repository = StudoraRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInitialNotesLoadedAndOrganizedBySubject() = runBlocking {
        val notes = repository.allNotes.first()
        assertTrue("Initial notes should contain sample items", notes.isNotEmpty())

        val subjects = notes.map { it.subject }.distinct()
        assertTrue(subjects.contains("Mathematics"))
        assertTrue(subjects.contains("Physics"))
        assertTrue(subjects.contains("Chemistry"))
        assertTrue(subjects.contains("Biology"))
        assertTrue(subjects.contains("Computer Science"))
        assertTrue(subjects.contains("Other"))
    }

    @Test
    fun testCreateAndSaveNoteLocally() = runBlocking {
        val note = StudyNote(
            title = "Optics & Snell's Law",
            subject = "Physics",
            chapter = "Chapter 8: Geometric Optics",
            content = "Snell's Law: n1 * sin(theta1) = n2 * sin(theta2). Total internal reflection occurs when angle > critical angle.",
            tags = "Optics, Physics, Formulas",
            isFavorite = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val insertedId = repository.insertNote(note)
        assertTrue(insertedId > 0)

        val retrievedNote = repository.getNoteById(insertedId.toInt())
        assertNotNull(retrievedNote)
        assertEquals("Optics & Snell's Law", retrievedNote?.title)
        assertEquals("Physics", retrievedNote?.subject)
        assertEquals("Chapter 8: Geometric Optics", retrievedNote?.chapter)
        assertTrue(retrievedNote?.content?.contains("Snell's Law") == true)
        assertTrue(retrievedNote?.isFavorite == true)
    }

    @Test
    fun testEditNote() = runBlocking {
        val notes = repository.allNotes.first()
        val original = notes.first()

        val updated = original.copy(
            title = "Updated Title for ${original.subject}",
            chapter = "Updated Chapter X",
            content = "Updated content formulas...",
            updatedAt = System.currentTimeMillis() + 1000L
        )

        repository.updateNote(updated)

        val reloaded = repository.getNoteById(original.id)
        assertNotNull(reloaded)
        assertEquals("Updated Title for ${original.subject}", reloaded?.title)
        assertEquals("Updated Chapter X", reloaded?.chapter)
        assertEquals("Updated content formulas...", reloaded?.content)
    }

    @Test
    fun testDeleteNote() = runBlocking {
        val initialNotes = repository.allNotes.first()
        val noteToDelete = initialNotes.first()
        val initialCount = initialNotes.size

        repository.deleteNote(noteToDelete)

        val updatedNotes = repository.allNotes.first()
        assertEquals(initialCount - 1, updatedNotes.size)
        assertNull(repository.getNoteById(noteToDelete.id))
    }

    @Test
    fun testFavoriteAndUnfavoriteNote() = runBlocking {
        val notes = repository.allNotes.first()
        val target = notes.first()
        val initialFav = target.isFavorite

        // Toggle 1: invert favorite
        repository.toggleFavoriteNote(target)
        val afterToggle = repository.getNoteById(target.id)
        assertEquals(!initialFav, afterToggle?.isFavorite)

        // Toggle 2: invert back
        repository.toggleFavoriteNote(afterToggle!!)
        val afterSecondToggle = repository.getNoteById(target.id)
        assertEquals(initialFav, afterSecondToggle?.isFavorite)
    }

    @Test
    fun testSearchNotesByTitleSubjectChapterAndContent() = runBlocking {
        val notes = repository.allNotes.first()

        // 1. Search by title keyword
        val calculusResults = notes.filter { note ->
            note.title.contains("Calculus", ignoreCase = true) ||
            note.subject.contains("Calculus", ignoreCase = true) ||
            note.chapter.contains("Calculus", ignoreCase = true) ||
            note.content.contains("Calculus", ignoreCase = true)
        }
        assertTrue(calculusResults.isNotEmpty())
        assertEquals("Mathematics", calculusResults.first().subject)

        // 2. Search by chapter keyword
        val chapterSearch = notes.filter { note ->
            note.title.contains("Mechanics", ignoreCase = true) ||
            note.subject.contains("Mechanics", ignoreCase = true) ||
            note.chapter.contains("Mechanics", ignoreCase = true) ||
            note.content.contains("Mechanics", ignoreCase = true)
        }
        assertTrue(chapterSearch.any { it.subject == "Physics" })

        // 3. Search by content keyword (e.g. "Krebs" or "Le Chatelier")
        val contentSearch = notes.filter { note ->
            note.title.contains("Krebs", ignoreCase = true) ||
            note.subject.contains("Krebs", ignoreCase = true) ||
            note.chapter.contains("Krebs", ignoreCase = true) ||
            note.content.contains("Krebs", ignoreCase = true)
        }
        assertTrue(contentSearch.any { it.subject == "Biology" })
    }

    @Test
    fun testFilterNotesBySubjectAndFavorites() = runBlocking {
        val notes = repository.allNotes.first()

        // Filter by subject
        val mathNotes = notes.filter { it.subject.equals("Mathematics", ignoreCase = true) }
        assertTrue(mathNotes.all { it.subject == "Mathematics" })

        val bioNotes = notes.filter { it.subject.equals("Biology", ignoreCase = true) }
        assertTrue(bioNotes.all { it.subject == "Biology" })

        // Filter by favorites
        val favNotes = notes.filter { it.isFavorite }
        assertTrue(favNotes.isNotEmpty())
        assertTrue(favNotes.all { it.isFavorite })
    }

    @Test
    fun testRecentNotesSortedByUpdatedAtDescending() = runBlocking {
        val notes = repository.allNotes.first()

        // StudyNoteDao orders by updatedAt DESC
        for (i in 0 until notes.size - 1) {
            assertTrue(
                "Notes should be sorted by updatedAt descending",
                notes[i].updatedAt >= notes[i + 1].updatedAt
            )
        }
    }
}
