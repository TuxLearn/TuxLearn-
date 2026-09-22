package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET"
        ) {
            Log.d("BootCompletedReceiver", "Triggered by $action. Rescheduling active reminders for current device timezone...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val activeTasks = db.studyTaskDao().getActiveReminderTasks()
                    val scheduler = StudyReminderScheduler(context)
                    val now = System.currentTimeMillis()
                    var count = 0
                    for (task in activeTasks) {
                        if (!task.isCompleted && task.reminderEnabled && task.reminderEpochMillis != null && task.reminderEpochMillis > now) {
                            scheduler.scheduleReminder(task)
                            count++
                        }
                    }
                    Log.d("BootCompletedReceiver", "Rescheduled $count reminders after boot.")
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "Failed to reschedule reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
