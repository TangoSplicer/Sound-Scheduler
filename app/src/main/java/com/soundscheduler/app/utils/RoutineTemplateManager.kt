// File: app/src/main/java/com/soundscheduler/app/utils/RoutineTemplateManager.kt
package com.soundscheduler.app.utils

// ← import PremiumManager so PremiumManager.isPremium() resolves
import com.soundscheduler.app.data.Routine

object RoutineTemplateManager {

    /**
     * Returns the list of built-in routine templates,
     * unlocking extra ones for premium users.
     */
    fun getAvailableTemplates(): List<Routine> {
        val templates = mutableListOf<Routine>()

        templates.add(Routine(title = "Morning Routine", type = "time", recurrence = "Daily"))
        templates.add(Routine(title = "Study Session", type = "time", recurrence = "Daily"))

        if (PremiumManager.isPremium()) {
            templates.add(Routine(title = "Workout Routine", type = "location", recurrence = "Weekly"))
            templates.add(Routine(title = "Meeting Prep", type = "calendar", recurrence = "Monthly"))
            templates.add(Routine(title = "Bedtime Routine", type = "time", recurrence = "Daily"))
        }

        return templates
    }
}
