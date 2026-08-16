package com.soundscheduler.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local singleton state for the user-visible automation lifecycle. Live foreground readiness is
 * always checked in the service; this table preserves only the latest explainable UI state.
 */
@Entity(tableName = "automation_state")
data class AutomationState(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val isPaused: Boolean = false,
    val lastArmedAtMillis: Long? = null,
    val lastActiveAtMillis: Long? = null,
    val lastStateCode: String = STATE_OFF,
    val lastStateDetailCode: String? = null,
    val pauseUntilMillis: Long? = null
) {
    init {
        require(id == SINGLETON_ID) { "Automation state must use the singleton ID" }
        require(lastStateCode in SUPPORTED_STATE_CODES) { "Unsupported automation state" }
    }

    companion object {
        const val SINGLETON_ID = 1
        const val STATE_ACTIVE = "active"
        const val STATE_PAUSED = "paused"
        const val STATE_OFF = "off"
        const val STATE_REARM_REQUIRED = "rearm_required"
        const val STATE_ACCESS_REQUIRED = "access_required"

        val SUPPORTED_STATE_CODES = setOf(
            STATE_ACTIVE,
            STATE_PAUSED,
            STATE_OFF,
            STATE_REARM_REQUIRED,
            STATE_ACCESS_REQUIRED
        )
    }
}
