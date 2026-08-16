package com.soundscheduler.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A privacy-bounded local record of one routine attempt. Coordinates, addresses, device IDs,
 * raw system messages, and other tracking data are intentionally excluded.
 */
@Entity(
    tableName = "routine_executions",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId", "occurredAtMillis"]),
        Index(value = ["occurredAtMillis"]),
        Index(value = ["outcomeCode", "occurredAtMillis"])
    ]
)
data class RoutineExecution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Int,
    val triggerType: String,
    val requestedMode: String,
    val occurredAtMillis: Long,
    val scheduledForAtMillis: Long? = null,
    val outcomeCode: String,
    val observedMode: String? = null,
    val detailCode: String? = null
) {
    init {
        require(routineId > 0) { "Routine execution requires a routine ID" }
        require(triggerType in SUPPORTED_TRIGGER_TYPES) { "Unsupported execution trigger: $triggerType" }
        require(requestedMode in Routine.SUPPORTED_SOUND_MODES) { "Unsupported requested sound mode" }
        require(occurredAtMillis > 0) { "Execution time must be positive" }
        require(outcomeCode in SUPPORTED_OUTCOME_CODES) { "Unsupported execution outcome: $outcomeCode" }
        require(observedMode == null || observedMode in Routine.SUPPORTED_SOUND_MODES) {
            "Unsupported observed sound mode"
        }
    }

    companion object {
        const val TRIGGER_TIME = "time"
        const val TRIGGER_LOCATION_ENTER = "location_enter"
        const val TRIGGER_LOCATION_EXIT = "location_exit"
        const val TRIGGER_CHARGING_CONNECTED = "charging_connected"
        const val TRIGGER_CHARGING_DISCONNECTED = "charging_disconnected"
        const val TRIGGER_BLUETOOTH_CONNECTED = "bluetooth_connected"
        const val TRIGGER_BLUETOOTH_DISCONNECTED = "bluetooth_disconnected"
        const val TRIGGER_WIFI_CONNECTED = "wifi_connected"
        const val TRIGGER_WIFI_DISCONNECTED = "wifi_disconnected"

        const val OUTCOME_APPLIED = "applied"
        const val OUTCOME_MODE_REJECTED = "mode_rejected"
        const val OUTCOME_ACCESS_REQUIRED = "access_required"
        const val OUTCOME_AUTOMATION_REARM_REQUIRED = "automation_rearm_required"
        const val OUTCOME_EXACT_ALARM_DEFERRED = "exact_alarm_deferred"
        const val OUTCOME_LOCATION_ACCESS_REQUIRED = "location_access_required"
        const val OUTCOME_LOCATION_UNAVAILABLE = "location_unavailable"
        const val OUTCOME_PAUSED = "paused"
        const val OUTCOME_INVALID_CONFIGURATION = "invalid_configuration"

        val SUPPORTED_TRIGGER_TYPES = setOf(
            TRIGGER_TIME,
            TRIGGER_LOCATION_ENTER,
            TRIGGER_LOCATION_EXIT,
            TRIGGER_CHARGING_CONNECTED,
            TRIGGER_CHARGING_DISCONNECTED,
            TRIGGER_BLUETOOTH_CONNECTED,
            TRIGGER_BLUETOOTH_DISCONNECTED,
            TRIGGER_WIFI_CONNECTED,
            TRIGGER_WIFI_DISCONNECTED
        )
        val SUPPORTED_OUTCOME_CODES = setOf(
            OUTCOME_APPLIED,
            OUTCOME_MODE_REJECTED,
            OUTCOME_ACCESS_REQUIRED,
            OUTCOME_AUTOMATION_REARM_REQUIRED,
            OUTCOME_EXACT_ALARM_DEFERRED,
            OUTCOME_LOCATION_ACCESS_REQUIRED,
            OUTCOME_LOCATION_UNAVAILABLE,
            OUTCOME_PAUSED,
            OUTCOME_INVALID_CONFIGURATION
        )
    }
}
