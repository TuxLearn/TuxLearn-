package com.example.data

import android.content.Context
import android.content.SharedPreferences

object ReminderPreferences {
    private const val PREF_NAME = "studora_reminder_prefs"
    private const val KEY_MASTER_ENABLED = "smart_reminders_master_enabled"
    private const val KEY_SNOOZE_MINUTES = "smart_reminders_snooze_minutes"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isMasterEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MASTER_ENABLED, true)
    }

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
    }

    fun getSnoozeMinutes(context: Context): Int {
        return getPrefs(context).getInt(KEY_SNOOZE_MINUTES, 15)
    }

    fun setSnoozeMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_SNOOZE_MINUTES, minutes).apply()
    }
}
