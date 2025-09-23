// File: app/src/main/java/com/soundscheduler/app/utils/PremiumManager.kt
package com.soundscheduler.app.utils

/**
 * Centralized manager for feature access.
 * All features are now available to all users.
 */
object PremiumManager {

    /**
     * @return Always returns true as all features are now available to all users.
     */
    fun isPremium(): Boolean {
        return true
    }

    /**
     * @return No limit on routines for all users.
     */
    fun getRoutineLimit(): Int = Int.MAX_VALUE
}