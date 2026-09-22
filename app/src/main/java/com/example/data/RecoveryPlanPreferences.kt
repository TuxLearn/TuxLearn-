package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class ActiveRecoveryPlan(
    val isActive: Boolean = false,
    val taskIds: List<Int> = emptyList(),
    val totalTasks: Int = 0,
    val daysPlanned: Int = 0,
    val totalStudyMinutes: Int = 0,
    val appliedEpoch: Long = 0L
)

object RecoveryPlanPreferences {
    private const val PREF_NAME = "studora_recovery_plan_prefs"
    private const val KEY_IS_ACTIVE = "recovery_is_active"
    private const val KEY_TASK_IDS = "recovery_task_ids"
    private const val KEY_TOTAL_TASKS = "recovery_total_tasks"
    private const val KEY_DAYS_PLANNED = "recovery_days_planned"
    private const val KEY_TOTAL_MINUTES = "recovery_total_minutes"
    private const val KEY_APPLIED_EPOCH = "recovery_applied_epoch"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getActivePlan(context: Context): ActiveRecoveryPlan? {
        val prefs = getPrefs(context)
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (!isActive) return null

        val idsString = prefs.getString(KEY_TASK_IDS, "") ?: ""
        val taskIds = idsString.split(",")
            .mapNotNull { it.trim().toIntOrNull() }

        val totalTasks = prefs.getInt(KEY_TOTAL_TASKS, taskIds.size)
        val daysPlanned = prefs.getInt(KEY_DAYS_PLANNED, 3)
        val totalMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0)
        val appliedEpoch = prefs.getLong(KEY_APPLIED_EPOCH, System.currentTimeMillis())

        return ActiveRecoveryPlan(
            isActive = true,
            taskIds = taskIds,
            totalTasks = totalTasks,
            daysPlanned = daysPlanned,
            totalStudyMinutes = totalMinutes,
            appliedEpoch = appliedEpoch
        )
    }

    fun setActivePlan(context: Context, plan: ActiveRecoveryPlan) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, plan.isActive)
            .putString(KEY_TASK_IDS, plan.taskIds.joinToString(","))
            .putInt(KEY_TOTAL_TASKS, plan.totalTasks)
            .putInt(KEY_DAYS_PLANNED, plan.daysPlanned)
            .putInt(KEY_TOTAL_MINUTES, plan.totalStudyMinutes)
            .putLong(KEY_APPLIED_EPOCH, plan.appliedEpoch)
            .apply()
    }

    fun clearActivePlan(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
