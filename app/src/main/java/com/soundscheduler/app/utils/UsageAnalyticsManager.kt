package com.soundscheduler.app.utils

import android.content.Context
import android.content.SharedPreferences

object UsageAnalyticsManager {

    private const val PREFS_NAME = "usage_analytics_prefs"
    private const val KEY_ROUTINES_CREATED = "routines_created"
    private const val KEY_ROUTINES_COMPLETED = "routines_completed"
    private const val KEY_NOTIFICATIONS_SENT = "notifications_sent"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun incrementRoutinesCreated(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_ROUTINES_CREATED, 0)
        prefs.edit().putInt(KEY_ROUTINES_CREATED, current + 1).apply()
    }

    fun incrementRoutinesCompleted(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_ROUTINES_COMPLETED, 0)
        prefs.edit().putInt(KEY_ROUTINES_COMPLETED, current + 1).apply()
    }

    fun incrementNotificationsSent(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_NOTIFICATIONS_SENT, 0)
        prefs.edit().putInt(KEY_NOTIFICATIONS_SENT, current + 1).apply()
    }

    fun getSummary(context: Context): String {
        val prefs = getPrefs(context)
        val created = prefs.getInt(KEY_ROUTINES_CREATED, 0)
        val completed = prefs.getInt(KEY_ROUTINES_COMPLETED, 0)
        val notifications = prefs.getInt(KEY_NOTIFICATIONS_SENT, 0)
        return "Created: $created | Completed: $completed | Notifications: $notifications"
    }
}