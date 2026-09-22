package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialData
import com.example.data.model.StudyTask
import com.example.data.repository.StudoraRepository
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
class StudyPlannerXpTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: StudoraRepository

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Seed initial sample data
        database.studyTaskDao().insertAll(InitialData.sampleTasks)
        database.studentProfileDao().insertOrUpdate(InitialData.sampleProfile)
        
        repository = StudoraRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testTaskCompletionAwardsXpExactlyOnceAndUncompletionDeductsIt() = runBlocking {
        val initialProfile = repository.profile.first()!!
        val baselineXp = initialProfile.xpPoints // 850
        val baselineCompletedTasks = initialProfile.tasksCompletedCount // 18

        val tasks = repository.allTasks.first()
        val uncompletedTask = tasks.first { !it.isCompleted }
        val taskXp = uncompletedTask.xpReward

        // 1. Mark completed
        val newStatus = repository.toggleTaskCompleted(uncompletedTask)
        assertTrue(newStatus == true)

        val completedProfile = repository.profile.first()!!
        assertEquals(baselineXp + taskXp, completedProfile.xpPoints)
        assertEquals(baselineCompletedTasks + 1, completedProfile.tasksCompletedCount)

        // 2. Mark uncompleted
        val uncompletedStatus = repository.toggleTaskCompleted(uncompletedTask.copy(isCompleted = true))
        assertTrue(uncompletedStatus == false)

        val revertedProfile = repository.profile.first()!!
        assertEquals(baselineXp, revertedProfile.xpPoints)
        assertEquals(baselineCompletedTasks, revertedProfile.tasksCompletedCount)

        // 3. Toggle multiple times repeatedly - verify no duplicate XP is ever generated
        for (i in 1..5) {
            val s1 = repository.toggleTaskCompleted(uncompletedTask)
            assertTrue(s1 == true)
            val p1 = repository.profile.first()!!
            assertEquals("XP mismatch after completion on loop $i", baselineXp + taskXp, p1.xpPoints)
            assertEquals("Count mismatch after completion on loop $i", baselineCompletedTasks + 1, p1.tasksCompletedCount)

            val s2 = repository.toggleTaskCompleted(uncompletedTask.copy(isCompleted = true))
            assertTrue(s2 == false)
            val p2 = repository.profile.first()!!
            assertEquals("XP mismatch after uncompletion on loop $i", baselineXp, p2.xpPoints)
            assertEquals("Count mismatch after uncompletion on loop $i", baselineCompletedTasks, p2.tasksCompletedCount)
        }
    }

    @Test
    fun testUncompletingAlreadyCompletedTaskRemovesAwardedXp() = runBlocking {
        val initialProfile = repository.profile.first()!!
        val baselineXp = initialProfile.xpPoints // 850
        val baselineCompletedTasks = initialProfile.tasksCompletedCount // 18

        val tasks = repository.allTasks.first()
        val alreadyCompletedTask = tasks.first { it.isCompleted } // Physics (25 XP)
        assertEquals(25, alreadyCompletedTask.xpReward)

        // Uncomplete the completed task
        val uncompletedStatus = repository.toggleTaskCompleted(alreadyCompletedTask)
        assertTrue(uncompletedStatus == false)

        val uncompletedProfile = repository.profile.first()!!
        assertEquals(baselineXp - 25, uncompletedProfile.xpPoints)
        assertEquals(baselineCompletedTasks - 1, uncompletedProfile.tasksCompletedCount)

        // Re-complete the task
        val recompletedStatus = repository.toggleTaskCompleted(alreadyCompletedTask.copy(isCompleted = false))
        assertTrue(recompletedStatus == true)

        val finalProfile = repository.profile.first()!!
        assertEquals(baselineXp, finalProfile.xpPoints)
        assertEquals(baselineCompletedTasks, finalProfile.tasksCompletedCount)
    }

    @Test
    fun testRedundantSetTaskCompletedDoesNotDuplicateXp() = runBlocking {
        val initialProfile = repository.profile.first()!!
        val baselineXp = initialProfile.xpPoints

        val tasks = repository.allTasks.first()
        val uncompletedTask = tasks.first { !it.isCompleted }

        // Set completed first time -> awards XP
        val firstResult = repository.setTaskCompleted(uncompletedTask.id, true)
        assertTrue(firstResult)
        assertEquals(baselineXp + uncompletedTask.xpReward, repository.profile.first()!!.xpPoints)

        // Redundant set completed second time -> must return false and NOT add XP
        val redundantResult = repository.setTaskCompleted(uncompletedTask.id, true)
        assertFalse(redundantResult)
        assertEquals(baselineXp + uncompletedTask.xpReward, repository.profile.first()!!.xpPoints)
    }

    @Test
    fun testTimeNeededAndCompletedCounts() = runBlocking {
        val initialTasks = repository.allTasks.first()
        val initialCompleted = initialTasks.count { it.isCompleted }
        val initialTimeNeeded = initialTasks.filter { !it.isCompleted }.sumOf { it.estimatedMinutes }

        val taskToToggle = initialTasks.first { !it.isCompleted }
        repository.toggleTaskCompleted(taskToToggle)

        val updatedTasks = repository.allTasks.first()
        val newCompleted = updatedTasks.count { it.isCompleted }
        val newTimeNeeded = updatedTasks.filter { !it.isCompleted }.sumOf { it.estimatedMinutes }

        assertEquals(initialCompleted + 1, newCompleted)
        assertEquals(initialTimeNeeded - taskToToggle.estimatedMinutes, newTimeNeeded)
    }

    @Test
    fun testAddNewTaskWithCustomPropertiesAndLocalPersistence() = runBlocking {
        val initialTasks = repository.allTasks.first()
        val initialCount = initialTasks.size

        val newTask = StudyTask(
            id = 0,
            title = "Linear Algebra Review",
            subject = "Mathematics",
            dueDate = "Today",
            estimatedMinutes = 60,
            priority = "High",
            xpReward = 45,
            reminderEnabled = true,
            reminderEpochMillis = System.currentTimeMillis() + 3600000L,
            reminderDateFormatted = "Today",
            reminderTimeFormatted = "04:30 PM",
            reminderRepeat = "None",
            reminderCategory = "Study session"
        )

        val insertedId = repository.insertTask(newTask)

        val updatedTasks = repository.allTasks.first()
        assertEquals(initialCount + 1, updatedTasks.size)
        val retrieved = updatedTasks.first { it.id == insertedId.toInt() || it.title == "Linear Algebra Review" }
        assertEquals("Linear Algebra Review", retrieved.title)
        assertEquals("Mathematics", retrieved.subject)
        assertEquals("Today", retrieved.dueDate)
        assertEquals(60, retrieved.estimatedMinutes)
        assertEquals(45, retrieved.xpReward)
        assertEquals("04:30 PM", retrieved.reminderTimeFormatted)
    }

    @Test
    fun testEditExistingTaskAndSynchronizeXp() = runBlocking {
        val tasks = repository.allTasks.first()
        val task = tasks.first { !it.isCompleted }

        // Complete the task first
        repository.toggleTaskCompleted(task)
        val profileAfterCompletion = repository.profile.first()!!
        val baselineXp = profileAfterCompletion.xpPoints

        // Edit the task to have higher XP reward (from task.xpReward to task.xpReward + 20)
        val updatedTask = task.copy(
            isCompleted = true,
            title = "Updated Title for Math",
            estimatedMinutes = 90,
            xpReward = task.xpReward + 20
        )
        repository.updateTask(updatedTask)

        // Verify task updated
        val retrieved = repository.allTasks.first().first { it.id == task.id }
        assertEquals("Updated Title for Math", retrieved.title)
        assertEquals(90, retrieved.estimatedMinutes)
        assertEquals(task.xpReward + 20, retrieved.xpReward)

        // Verify profile XP updated atomically
        val profileAfterEdit = repository.profile.first()!!
        assertEquals(baselineXp + 20, profileAfterEdit.xpPoints)
    }

    @Test
    fun testDeleteTaskRemovesItAndUpdatesMetrics() = runBlocking {
        val initialTasks = repository.allTasks.first()
        val initialCount = initialTasks.size
        val taskToDelete = initialTasks.first { !it.isCompleted }
        val initialTimeNeeded = initialTasks.filter { !it.isCompleted }.sumOf { it.estimatedMinutes }

        repository.deleteTask(taskToDelete)

        val updatedTasks = repository.allTasks.first()
        assertEquals(initialCount - 1, updatedTasks.size)
        assertFalse(updatedTasks.any { it.id == taskToDelete.id })

        val newTimeNeeded = updatedTasks.filter { !it.isCompleted }.sumOf { it.estimatedMinutes }
        assertEquals(initialTimeNeeded - taskToDelete.estimatedMinutes, newTimeNeeded)
    }

    @Test
    fun testSubjectFilterAndSearchLogic() = runBlocking {
        val tasks = repository.allTasks.first()
        
        // Subject filtering
        val mathTasks = tasks.filter { it.subject.equals("Mathematics", ignoreCase = true) }
        assertTrue(mathTasks.isNotEmpty())
        assertTrue(mathTasks.all { it.subject == "Mathematics" })

        // Search filtering by title
        val searchByTitle = tasks.filter {
            it.title.contains("Calculus", ignoreCase = true) || it.subject.contains("Calculus", ignoreCase = true)
        }
        assertTrue(searchByTitle.isNotEmpty())
        assertTrue(searchByTitle.all { it.title.contains("Calculus", ignoreCase = true) || it.subject.contains("Calculus", ignoreCase = true) })

        // Search filtering by subject
        val searchBySubject = tasks.filter {
            it.title.contains("Physics", ignoreCase = true) || it.subject.contains("Physics", ignoreCase = true)
        }
        assertTrue(searchBySubject.isNotEmpty())
    }
}
