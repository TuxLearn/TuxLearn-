package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.InitialData
import com.example.data.model.StudyTask
import com.example.data.repository.StudoraRepository
import com.example.notification.StudyReminderReceiver
import com.example.notification.StudyReminderScheduler
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyReminderTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var scheduler: StudyReminderScheduler
    private lateinit var repository: StudoraRepository
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        database.studyTaskDao().insertAll(InitialData.sampleTasks)
        database.studentProfileDao().insertOrUpdate(InitialData.sampleProfile)

        scheduler = StudyReminderScheduler(context)
        repository = StudoraRepository(database, scheduler)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testScheduleReminderWhenOnAndFuture() = runBlocking {
        val futureTime = System.currentTimeMillis() + 1800000L // 30 mins future
        val task = StudyTask(
            title = "Maths Chapter 1",
            subject = "Mathematics",
            dueDate = "Today",
            estimatedMinutes = 45,
            priority = "High",
            reminderEnabled = true,
            reminderEpochMillis = futureTime,
            reminderDateFormatted = "Today",
            reminderTimeFormatted = "6:30 PM"
        )

        val insertedId = repository.insertTask(task)
        assertTrue(insertedId > 0)

        // Verify alarm was scheduled in ShadowAlarmManager
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())
        val matchingAlarm = scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId.toInt()
        }
        assertNotNull("Expected scheduled alarm for task $insertedId", matchingAlarm)
        assertEquals(futureTime, matchingAlarm!!.triggerAtTime)
    }

    @Test
    fun testDoNotScheduleWhenReminderOff() = runBlocking {
        val initialAlarmCount = shadowAlarmManager.scheduledAlarms.size
        val futureTime = System.currentTimeMillis() + 1800000L
        val task = StudyTask(
            title = "Chemistry Lab Notes",
            subject = "Chemistry",
            dueDate = "Tomorrow",
            reminderEnabled = false,
            reminderEpochMillis = futureTime
        )

        val insertedId = repository.insertTask(task)
        assertTrue(insertedId > 0)

        val matchingAlarm = shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId.toInt()
        }
        assertNull("Should not schedule alarm when reminder is OFF", matchingAlarm)
    }

    @Test
    fun testNeverScheduleInPast() = runBlocking {
        val pastTime = System.currentTimeMillis() - 60000L // 1 minute in the past
        val task = StudyTask(
            id = 999,
            title = "Past Task",
            subject = "History",
            dueDate = "Today",
            reminderEnabled = true,
            reminderEpochMillis = pastTime
        )

        val scheduled = scheduler.scheduleReminder(task)
        assertFalse("Scheduler should reject past timestamps", scheduled)
    }

    @Test
    fun testCancelReminderWhenTaskCompleted() = runBlocking {
        val futureTime = System.currentTimeMillis() + 3600000L
        val task = StudyTask(
            title = "Biology Review",
            subject = "Biology",
            dueDate = "Today",
            reminderEnabled = true,
            reminderEpochMillis = futureTime
        )

        val insertedId = repository.insertTask(task).toInt()
        val savedTask = repository.getTaskById(insertedId)!!

        // Verify alarm is present
        var matchingAlarm = shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId
        }
        assertNotNull(matchingAlarm)

        // Mark task completed
        repository.toggleTaskCompleted(savedTask)

        // Verify alarm has been cancelled
        matchingAlarm = shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId
        }
        assertNull("Alarm should be cancelled upon task completion", matchingAlarm)
    }

    @Test
    fun testCancelReminderWhenTaskDeleted() = runBlocking {
        val futureTime = System.currentTimeMillis() + 7200000L
        val task = StudyTask(
            title = "Literature Essay",
            subject = "Literature",
            dueDate = "Tomorrow",
            reminderEnabled = true,
            reminderEpochMillis = futureTime
        )

        val insertedId = repository.insertTask(task).toInt()
        val savedTask = repository.getTaskById(insertedId)!!

        assertNotNull(shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId
        })

        // Delete task
        repository.deleteTask(savedTask)

        // Verify alarm is cancelled
        assertNull(shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId
        })
    }

    @Test
    fun testRescheduleReminderWhenTaskEdited() = runBlocking {
        val initialFutureTime = System.currentTimeMillis() + 1800000L
        val task = StudyTask(
            title = "Maths Chapter 1",
            subject = "Mathematics",
            dueDate = "Today",
            reminderEnabled = true,
            reminderEpochMillis = initialFutureTime
        )

        val insertedId = repository.insertTask(task).toInt()
        val savedTask = repository.getTaskById(insertedId)!!

        val alarm1 = shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId
        }
        assertNotNull(alarm1)
        assertEquals(initialFutureTime, alarm1!!.triggerAtTime)

        // Edit reminder time
        val updatedFutureTime = System.currentTimeMillis() + 3600000L
        val editedTask = savedTask.copy(
            reminderEpochMillis = updatedFutureTime,
            reminderTimeFormatted = "7:30 PM"
        )
        repository.updateTask(editedTask)

        val alarm2 = shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == insertedId
        }
        assertNotNull(alarm2)
        assertEquals("Alarm should be updated to new trigger time", updatedFutureTime, alarm2!!.triggerAtTime)
    }

    @Test
    fun testCompleteMathsChapter1NearFutureJourney() = runBlocking {
        // 1. Create "Maths Chapter 1" task with near-future reminder
        val nearFutureTime = System.currentTimeMillis() + 900000L // 15 mins
        val task = StudyTask(
            title = "Maths Chapter 1",
            subject = "Mathematics",
            dueDate = "Today",
            estimatedMinutes = 30,
            priority = "High",
            reminderEnabled = true,
            reminderEpochMillis = nearFutureTime,
            reminderDateFormatted = "Today",
            reminderTimeFormatted = "5:15 PM"
        )

        val taskId = repository.insertTask(task).toInt()
        var currentTask = repository.getTaskById(taskId)!!
        assertEquals("Maths Chapter 1", currentTask.title)
        assertTrue(currentTask.reminderEnabled)

        // Verify alarm scheduled
        fun getAlarm() = shadowAlarmManager.scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == taskId
        }
        assertNotNull(getAlarm())
        assertEquals(nearFutureTime, getAlarm()!!.triggerAtTime)

        // 2. Reschedule on edit
        val rescheduledTime = System.currentTimeMillis() + 1800000L
        repository.updateTask(currentTask.copy(reminderEpochMillis = rescheduledTime))
        assertEquals(rescheduledTime, getAlarm()!!.triggerAtTime)

        // 3. Mark completed -> cancel pending reminder
        repository.setTaskCompleted(taskId, true)
        assertNull("Alarm should be cancelled on task completion", getAlarm())

        // 4. Mark uncompleted -> reschedule reminder
        repository.setTaskCompleted(taskId, false)
        assertNotNull("Alarm should be rescheduled on task uncompletion", getAlarm())

        // 5. Delete task -> cancel reminder
        currentTask = repository.getTaskById(taskId)!!
        repository.deleteTask(currentTask)
        assertNull("Alarm should be cancelled on task deletion", getAlarm())
    }

    @Test
    fun testScheduleTestNotificationIn10Seconds() {
        val before = System.currentTimeMillis()
        val scheduled = scheduler.scheduleTestNotificationIn10Seconds()
        val after = System.currentTimeMillis()
        assertTrue("Expected 10s test notification scheduling to succeed", scheduled)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        val testAlarm = scheduledAlarms.firstOrNull {
            val intent = shadowOf(it.operation).savedIntent
            intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1) == 999998
        }
        assertNotNull("Expected scheduled alarm for 10-second test notification", testAlarm)
        assertTrue(
            "Alarm trigger time should be ~10 seconds from now",
            testAlarm!!.triggerAtTime in (before + 9000L)..(after + 11000L)
        )

        val intent = shadowOf(testAlarm.operation).savedIntent
        assertTrue(intent.getBooleanExtra(StudyReminderScheduler.EXTRA_IS_TEST, false))
        assertEquals("Study Reminder Test", intent.getStringExtra(StudyReminderScheduler.EXTRA_CUSTOM_TITLE))
        assertEquals("Your Smart Reminder notification is working.", intent.getStringExtra(StudyReminderScheduler.EXTRA_CUSTOM_BODY))
    }
}
