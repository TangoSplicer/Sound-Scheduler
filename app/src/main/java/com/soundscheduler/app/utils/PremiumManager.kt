// File: app/src/main/java/com/soundscheduler/app/utils/PremiumManager.kt
package com.soundscheduler.app.utils

/**
 * Centralized manager for premium-tier checks and feature limits.
 */
object PremiumManager {

    // Max routines free users can create
    private const val FREE_ROUTINE_LIMIT = 5

    /**
     * @return true if the user has unlocked premium.
     * TODO: Hook up to your billing/entitlement check.
     */
    fun isPremium(): Boolean {
        return BuildConfig.BETA
    }

    /**
     * @return maximum routines allowed for this user.
     */
    fun getRoutineLimit(): Int =
        if (isPremium()) Int.MAX_VALUE else FREE_ROUTINE_LIMIT
}
