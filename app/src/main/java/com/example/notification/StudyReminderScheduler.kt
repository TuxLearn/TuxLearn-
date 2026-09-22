package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.ReminderPreferences
import com.example.data.model.StudyTask

class StudyReminderScheduler(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "study_reminders"
        const val CHANNEL_NAME = "Study Reminders"
        const val ACTION_STUDY_REMINDER = "com.example.ACTION_STUDY_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.ACTION_SNOOZE_REMINDER"
        const val ACTION_MARK_DONE = "com.example.ACTION_MARK_DONE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_TASK_SUBJECT = "task_subject"
        const val EXTRA_TASK_CATEGORY = "task_category"
        const val EXTRA_TASK_PRIORITY = "task_priority"
        const val EXTRA_TASK_MINUTES = "task_minutes"
        const val EXTRA_IS_TEST = "is_test"
        const val EXTRA_CUSTOM_TITLE = "custom_title"
        const val EXTRA_CUSTOM_BODY = "custom_body"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel != null) {
                    return
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts and reminders for scheduled study tasks"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                    setSound(defaultSoundUri, audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun openExactAlarmSettings(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        Log.e("StudyReminderScheduler", "Unable to open exact alarm settings", e2)
                    }
                }
            }
        }
    }

    init {
        createNotificationChannel(context)
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun scheduleReminder(task: StudyTask): Boolean {
        // Requirement 9: Check global master toggle
        if (!ReminderPreferences.isMasterEnabled(context)) {
            Log.d("StudyReminderScheduler", "Smart reminders master switch is OFF. Skipping alarm for task ${task.id}")
            cancelReminder(task.id)
            return false
        }

        // If reminder is OFF, do not schedule
        if (!task.reminderEnabled || task.reminderEpochMillis == null || task.isCompleted) {
            cancelReminder(task.id)
            return false
        }

        val triggerTime = task.reminderEpochMillis
        val now = System.currentTimeMillis()
        if (triggerTime <= now) {
            Log.w("StudyReminderScheduler", "Skipping reminder for task ${task.id}: trigger time $triggerTime is in the past ($now)")
            return false
        }

        if (alarmManager == null) {
            Log.e("StudyReminderScheduler", "AlarmManager not available")
            return false
        }

        // Cancel any existing reminder for this task first to avoid duplicates
        cancelReminder(task.id)

        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            data = Uri.parse("tuxlearn://task/${task.id}")
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_SUBJECT, task.subject)
            putExtra(EXTRA_TASK_CATEGORY, task.reminderCategory)
            putExtra(EXTRA_TASK_PRIORITY, task.priority)
            putExtra(EXTRA_TASK_MINUTES, task.estimatedMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("StudyReminderScheduler", "Scheduled exact reminder for task ${task.id} at $triggerTime (repeat: ${task.reminderRepeat})")
            return true
        } catch (e: SecurityException) {
            Log.e("StudyReminderScheduler", "Exact alarm permission not granted, fallback to standard alarm", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                return true
            } catch (fallbackEx: Exception) {
                Log.e("StudyReminderScheduler", "Fallback alarm failed", fallbackEx)
                return false
            }
        } catch (e: Exception) {
            Log.e("StudyReminderScheduler", "Failed to schedule reminder for task ${task.id}", e)
            return false
        }
    }

    fun cancelReminder(taskId: Int) {
        if (taskId <= 0) return

        // 1. Cancel alarm pending intent
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            data = Uri.parse("tuxlearn://task/$taskId")
            putExtra(EXTRA_TASK_ID, taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("StudyReminderScheduler", "Cancelled scheduled alarm for task $taskId")
        }

        // 2. Dismiss any active notification for this task
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(taskId)
        } catch (e: Exception) {
            Log.e("StudyReminderScheduler", "Failed to cancel notification for task $taskId", e)
        }
    }

    fun cancelAllReminders(tasks: List<StudyTask>) {
        tasks.forEach { cancelReminder(it.id) }
    }

    /**
     * Immediately triggers an in-preview notification test using the exact same Android
     * notification system, channel, and action configuration as scheduled reminders.
     * Title: "Study Reminder Test"
     * Body: "Your Smart Reminder notifications are working correctly."
     */
    fun sendTestNotification(task: StudyTask? = null): Boolean {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (perm != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("StudyReminderScheduler", "POST_NOTIFICATIONS permission not granted. Cannot post test notification.")
                return false
            }
        }

        val testTaskId = task?.id ?: 99999
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_screen", "REMINDERS")
            putExtra("target_task_id", testTaskId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            testTaskId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            data = Uri.parse("tuxlearn://snooze/$testTaskId")
            putExtra(EXTRA_TASK_ID, testTaskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            testTaskId + 100000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            data = Uri.parse("tuxlearn://done/$testTaskId")
            putExtra(EXTRA_TASK_ID, testTaskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            testTaskId + 200000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "Study Reminder Test"
        val notificationBody = "Your Smart Reminder notifications are working correctly."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(notificationTitle)
                    .bigText(notificationBody)
            )
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

        return try {
            NotificationManagerCompat.from(context).notify(testTaskId, notification)
            Log.d("StudyReminderScheduler", "Real test alert notification posted successfully with ID $testTaskId")
            true
        } catch (e: SecurityException) {
            Log.e("StudyReminderScheduler", "SecurityException posting notification", e)
            false
        } catch (e: Exception) {
            Log.e("StudyReminderScheduler", "Error showing test notification", e)
            false
        }
    }

    /**
     * Schedules a REAL Android local notification for exactly 10 seconds from now
     * using AlarmManager and the same notification channel/receiver mechanism as normal reminders.
     * Title: "Study Reminder Test"
     * Body: "Your Smart Reminder notification is working."
     */
    fun scheduleTestNotificationIn10Seconds(): Boolean {
        createNotificationChannel(context)

        val triggerTime = System.currentTimeMillis() + 10_000L
        val testTaskId = 999998

        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_STUDY_REMINDER
            data = Uri.parse("tuxlearn://test-10s/$testTaskId")
            putExtra(EXTRA_TASK_ID, testTaskId)
            putExtra(EXTRA_IS_TEST, true)
            putExtra(EXTRA_CUSTOM_TITLE, "Study Reminder Test")
            putExtra(EXTRA_CUSTOM_BODY, "Your Smart Reminder notification is working.")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            testTaskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (alarmManager == null) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("StudyReminderScheduler", "Scheduled real 10-second test notification at $triggerTime")
            true
        } catch (e: Exception) {
            Log.e("StudyReminderScheduler", "Error scheduling 10-second test notification", e)
            false
        }
    }
}
