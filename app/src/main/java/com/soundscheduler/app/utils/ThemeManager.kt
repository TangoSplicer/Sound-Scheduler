package com.soundscheduler.app.utils

import android.content.Context
import android.content.SharedPreferences

object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    fun setTheme(context: Context, theme: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, "light") ?: "light"
    }
}