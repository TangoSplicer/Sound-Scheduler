package com.soundscheduler.app.utils

import android.content.Context import android.content.SharedPreferences

object PremiumManager {

private const val PREFS_NAME = "premium_prefs"
private const val KEY_IS_PREMIUM = "is_premium"
private const val KEY_ROUTINE_LIMIT = "routine_limit"

private const val FREE_ROUTINE_LIMIT = 3
private const val PREMIUM_ROUTINE_LIMIT = 10

private lateinit var prefs: SharedPreferences

fun initialize(context: Context) {
    prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun setPremium(isPremium: Boolean) {
    prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
    prefs.edit().putInt(KEY_ROUTINE_LIMIT, if (isPremium) PREMIUM_ROUTINE_LIMIT else FREE_ROUTINE_LIMIT).apply()
}

fun isPremium(): Boolean = prefs.getBoolean(KEY_IS_PREMIUM, false)

fun getRoutineLimit(): Int =
    prefs.getInt(KEY_ROUTINE_LIMIT, if (isPremium()) PREMIUM_ROUTINE_LIMIT else FREE_ROUTINE_LIMIT)

}

