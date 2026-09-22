package com.example.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ReminderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_ID, -1)
        if (taskId <= 0) return

        val action = intent.action ?: StudyReminderScheduler.ACTION_STUDY_REMINDER
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val scheduler = StudyReminderScheduler(context)

                when (action) {
                    StudyReminderScheduler.ACTION_SNOOZE_REMINDER -> {
                        // Requirement 6: "Remind me later"
                        Log.d("StudyReminderReceiver", "Handling 'Remind me later' for task $taskId")
                        NotificationManagerCompat.from(context).cancel(taskId)

                        val task = db.studyTaskDao().getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            val snoozeMins = ReminderPreferences.getSnoozeMinutes(context)
                            val snoozeMillis = System.currentTimeMillis() + snoozeMins * 60 * 1000L
                            val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                            val dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                            val snoozeDate = Date(snoozeMillis)

                            val updatedTask = task.copy(
                                reminderEnabled = true,
                                reminderEpochMillis = snoozeMillis,
                                reminderDateFormatted = dateFormatter.format(snoozeDate),
                                reminderTimeFormatted = timeFormatter.format(snoozeDate)
                            )
                            db.studyTaskDao().updateTask(updatedTask)
                            scheduler.scheduleReminder(updatedTask)
                            Log.d("StudyReminderReceiver", "Task $taskId snoozed to $snoozeMillis")
                        }
                    }

                    StudyReminderScheduler.ACTION_MARK_DONE -> {
                        // Requirement 6: "Mark as Done"
                        Log.d("StudyReminderReceiver", "Handling 'Mark as Done' for task $taskId")
                        NotificationManagerCompat.from(context).cancel(taskId)

                        val task = db.studyTaskDao().getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            db.studyTaskDao().setTaskCompleted(
                                id = task.id,
                                isCompleted = true,
                                completedAt = System.currentTimeMillis()
                            )
                            db.studentProfileDao().addXp(task.xpReward)
                            db.studentProfileDao().incrementCompletedTasks()
                            scheduler.cancelReminder(task.id)
                            Log.d("StudyReminderReceiver", "Task $taskId marked as completed via notification action (+${task.xpReward} XP)")
                        }
                    }

                    else -> {
                        // Requirement 4 & 5: Trigger notification
                        // Notification Title & Body
                        val isTest = intent.getBooleanExtra(StudyReminderScheduler.EXTRA_IS_TEST, false)
                        val customTitle = intent.getStringExtra(StudyReminderScheduler.EXTRA_CUSTOM_TITLE)
                        val customBody = intent.getStringExtra(StudyReminderScheduler.EXTRA_CUSTOM_BODY)

                        if (!isTest && !ReminderPreferences.isMasterEnabled(context)) {
                            Log.d("StudyReminderReceiver", "Smart reminders master switch is OFF. Suppressing notification.")
                            return@launch
                        }

                        StudyReminderScheduler.createNotificationChannel(context)
                        val task = db.studyTaskDao().getTaskById(taskId)

                        if (!isTest && (task == null || task.isCompleted || !task.reminderEnabled)) {
                            Log.d("StudyReminderReceiver", "Task $taskId is completed, deleted, or reminder disabled. Notification suppressed.")
                            return@launch
                        }

                        val title = task?.title ?: intent.getStringExtra(StudyReminderScheduler.EXTRA_TASK_TITLE) ?: "Study Session"
                        val subject = task?.subject ?: intent.getStringExtra(StudyReminderScheduler.EXTRA_TASK_SUBJECT) ?: "General"
                        val category = (task?.reminderCategory ?: intent.getStringExtra(StudyReminderScheduler.EXTRA_TASK_CATEGORY) ?: "Study session").ifBlank { "Study session" }
                        val estimatedMinutes = task?.estimatedMinutes ?: intent.getIntExtra(StudyReminderScheduler.EXTRA_TASK_MINUTES, 30)
                        val taskPriority = task?.priority ?: intent.getStringExtra(StudyReminderScheduler.EXTRA_TASK_PRIORITY) ?: "Medium"

                        // Check permission on Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                Log.w("StudyReminderReceiver", "POST_NOTIFICATIONS permission not granted. Cannot display notification.")
                                return@launch
                            }
                        }

                        // Content Intent (tap opens Study Planner or Smart Reminders)
                        val contentIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("nav_screen", "REMINDERS")
                            putExtra("target_task_id", taskId)
                        }

                        val contentPendingIntent = PendingIntent.getActivity(
                            context,
                            taskId,
                            contentIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // Action 1: "Remind me later"
                        val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
                            this.action = StudyReminderScheduler.ACTION_SNOOZE_REMINDER
                            data = Uri.parse("tuxlearn://snooze/$taskId")
                            putExtra(StudyReminderScheduler.EXTRA_TASK_ID, taskId)
                        }
                        val snoozePendingIntent = PendingIntent.getBroadcast(
                            context,
                            taskId + 100000,
                            snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // Action 2: "Mark as Done"
                        val doneIntent = Intent(context, StudyReminderReceiver::class.java).apply {
                            this.action = StudyReminderScheduler.ACTION_MARK_DONE
                            data = Uri.parse("tuxlearn://done/$taskId")
                            putExtra(StudyReminderScheduler.EXTRA_TASK_ID, taskId)
                        }
                        val donePendingIntent = PendingIntent.getBroadcast(
                            context,
                            taskId + 200000,
                            doneIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val finalTitle = when {
                            !customTitle.isNullOrBlank() -> customTitle
                            isTest -> "Study Reminder Test"
                            else -> "Time to study: $title"
                        }
                        val finalBody = when {
                            !customBody.isNullOrBlank() -> customBody
                            isTest -> "Your Smart Reminder notification is working."
                            else -> "Subject: $subject • $category • Time to study!"
                        }

                        val notificationStyle = if (isTest) {
                            NotificationCompat.BigTextStyle()
                                .setBigContentTitle(finalTitle)
                                .bigText(finalBody)
                        } else {
                            NotificationCompat.BigTextStyle()
                                .setBigContentTitle("Time to study: $title")
                                .bigText("Time to study!\nSubject: $subject\nType: $category\nStudy Task: $title\nDuration: ${estimatedMinutes}m • Priority: $taskPriority")
                                .setSummaryText("$subject • $category")
                        }

                        val notification = NotificationCompat.Builder(context, StudyReminderScheduler.CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_popup_reminder)
                            .setContentTitle(finalTitle)
                            .setContentText(finalBody)
                            .setStyle(notificationStyle)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setAutoCancel(true)
                            .setContentIntent(contentPendingIntent)
                            .addAction(
                                android.R.drawable.ic_popup_sync,
                                "Remind me later",
                                snoozePendingIntent
                            )
                            .addAction(
                                android.R.drawable.checkbox_on_background,
                                "Mark as Done",
                                donePendingIntent
                            )
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .build()

                        NotificationManagerCompat.from(context).notify(taskId, notification)
                        Log.d("StudyReminderReceiver", "Notification posted successfully for task $taskId")

                        // Requirement 3: Handle repeating reminder (Daily, Weekly / Custom)
                        if (task != null) {
                            val now = System.currentTimeMillis()
                            when (task.reminderRepeat) {
                                "Daily" -> {
                                    val nextEpoch = (task.reminderEpochMillis ?: now) + 24 * 60 * 60 * 1000L
                                    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                                    val dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                                    val nextDate = Date(nextEpoch)
                                    val repeatingTask = task.copy(
                                        reminderEpochMillis = nextEpoch,
                                        reminderDateFormatted = dateFormatter.format(nextDate),
                                        reminderTimeFormatted = timeFormatter.format(nextDate)
                                    )
                                    db.studyTaskDao().updateTask(repeatingTask)
                                    scheduler.scheduleReminder(repeatingTask)
                                    Log.d("StudyReminderReceiver", "Scheduled daily repeat for task $taskId at $nextEpoch")
                                }
                                "Weekly", "Custom" -> {
                                    val nextEpoch = (task.reminderEpochMillis ?: now) + 7 * 24 * 60 * 60 * 1000L
                                    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                                    val dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                                    val nextDate = Date(nextEpoch)
                                    val repeatingTask = task.copy(
                                        reminderEpochMillis = nextEpoch,
                                        reminderDateFormatted = dateFormatter.format(nextDate),
                                        reminderTimeFormatted = timeFormatter.format(nextDate)
                                    )
                                    db.studyTaskDao().updateTask(repeatingTask)
                                    scheduler.scheduleReminder(repeatingTask)
                                    Log.d("StudyReminderReceiver", "Scheduled repeat for task $taskId at $nextEpoch")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StudyReminderReceiver", "Error processing reminder event for task $taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
