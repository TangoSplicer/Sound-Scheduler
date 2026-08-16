package com.soundscheduler.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String,
    val time: Long? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int? = null,
    val locationTransition: String? = null,
    val chargingTransition: String? = null,
    val bluetoothDeviceAddress: String? = null,
    val wifiSsid: String? = null,
    val calendarEventId: String? = null,
    val calendarKeyword: String? = null,
    val calendarBufferMinutes: Int = 5,
    val batteryThreshold: Int? = null,
    val batteryTriggerDirection: String? = null,
    val webhookUrl: String? = null,
    val isCompleted: Boolean = false,
    val recurrence: String? = null,
    val soundProfile: String = PROFILE_RING,
    val isEnabled: Boolean = true,
    val lastAttemptAtMillis: Long? = null,
    val lastOutcomeAtMillis: Long? = null,
    val lastOutcomeCode: String? = null,
    val lastObservedMode: String? = null,
    val lastOutcomeDetailCode: String? = null,
    val lastExecutionId: Long? = null,
    val wasEnabledBeforeGlobalPause: Boolean = false,
    val daysOfWeek: String? = null
) {
    init {
        require(title.isNotBlank()) { "Routine title cannot be blank" }
        require(title.length <= MAX_TITLE_LENGTH) { "Routine title cannot exceed $MAX_TITLE_LENGTH characters" }
        require(type in SUPPORTED_TYPES) { "Unsupported routine type: $type" }
        require(soundProfile in SUPPORTED_STORED_SOUND_PROFILES) {
            "Unsupported sound profile: $soundProfile"
        }
        require(recurrence == null || recurrence in SUPPORTED_RECURRENCES) {
            "Unsupported recurrence type: $recurrence"
        }
        require(lastOutcomeCode == null || lastOutcomeCode in RoutineExecution.SUPPORTED_OUTCOME_CODES) {
            "Unsupported last outcome type: $lastOutcomeCode"
        }
        require(lastObservedMode == null || lastObservedMode in SUPPORTED_SOUND_MODES) {
            "Unsupported observed sound mode: $lastObservedMode"
        }
        require(lastExecutionId == null || lastExecutionId > 0) {
            "Last execution ID must be positive"
        }

        when (type) {
            TYPE_TIME -> require(time != null && time > 0) {
                "Time-based routines require a positive trigger time"
            }
            TYPE_LOCATION -> require(!location.isNullOrBlank()) {
                "Location-based routines require a location"
            }
            TYPE_CHARGING -> require(chargingTransition in SUPPORTED_CHARGING_TRANSITIONS) {
                "Charging routines require a supported power transition"
            }
            TYPE_BLUETOOTH -> require(!bluetoothDeviceAddress.isNullOrBlank()) {
                "Bluetooth routines require a device address"
            }
            TYPE_WIFI -> require(!wifiSsid.isNullOrBlank()) {
                "Wi-Fi routines require an SSID"
            }
            TYPE_CALENDAR -> require(!calendarEventId.isNullOrBlank() || !calendarKeyword.isNullOrBlank()) {
                "Calendar-based routines require a calendar event ID or keyword filter"
            }
            TYPE_BATTERY -> require(batteryThreshold != null && batteryThreshold in 1..100) {
                "Battery routines require a valid threshold between 1 and 100"
            }
        }
    }

    fun hasUsableLocation(): Boolean =
        type == TYPE_LOCATION &&
            !location.isNullOrBlank() &&
            latitude != null && latitude in -90.0..90.0 &&
            longitude != null && longitude in -180.0..180.0 &&
            radiusMeters != null && radiusMeters in MIN_LOCATION_RADIUS_METERS..MAX_LOCATION_RADIUS_METERS &&
            locationTransition in SUPPORTED_LOCATION_TRANSITIONS

    fun targetSoundMode(): String = when (soundProfile) {
        PROFILE_SILENT -> PROFILE_SILENT
        PROFILE_VIBRATE -> PROFILE_VIBRATE
        PROFILE_RING, PROFILE_NORMAL, PROFILE_CUSTOM -> PROFILE_RING
        else -> PROFILE_RING
    }

    companion object {
        const val TYPE_TIME = "time"
        const val TYPE_LOCATION = "location"
        const val TYPE_CHARGING = "charging"
        const val TYPE_BLUETOOTH = "bluetooth"
        const val TYPE_WIFI = "wifi"
        const val TYPE_BATTERY = "battery"
        const val TYPE_CALENDAR = "calendar"

        const val RECURRENCE_DAILY = "daily"
        const val RECURRENCE_WEEKLY = "weekly"
        const val RECURRENCE_MONTHLY = "monthly"

        const val PROFILE_RING = "ring"
        const val PROFILE_SILENT = "silent"
        const val PROFILE_VIBRATE = "vibrate"

        const val LOCATION_TRANSITION_ENTER = "enter"
        const val LOCATION_TRANSITION_EXIT = "exit"
        const val CHARGING_TRANSITION_CONNECTED = "connected"
        const val CHARGING_TRANSITION_DISCONNECTED = "disconnected"
        const val DEFAULT_LOCATION_RADIUS_METERS = 150
        const val MIN_LOCATION_RADIUS_METERS = 100
        const val MAX_LOCATION_RADIUS_METERS = 5_000

        // Legacy values remain readable so existing on-device records keep working.
        const val PROFILE_NORMAL = "normal"
        const val PROFILE_CUSTOM = "custom"

        const val MAX_TITLE_LENGTH = 100

        val SUPPORTED_TYPES = setOf(TYPE_TIME, TYPE_LOCATION, TYPE_CHARGING, TYPE_BLUETOOTH, TYPE_WIFI, TYPE_BATTERY, TYPE_CALENDAR)
        val SUPPORTED_RECURRENCES = setOf(RECURRENCE_DAILY, RECURRENCE_WEEKLY, RECURRENCE_MONTHLY)
        val SUPPORTED_SOUND_MODES = setOf(PROFILE_RING, PROFILE_SILENT, PROFILE_VIBRATE)
        val SUPPORTED_LOCATION_TRANSITIONS = setOf(LOCATION_TRANSITION_ENTER, LOCATION_TRANSITION_EXIT)
        val SUPPORTED_CHARGING_TRANSITIONS = setOf(
            CHARGING_TRANSITION_CONNECTED,
            CHARGING_TRANSITION_DISCONNECTED
        )
        val SUPPORTED_STORED_SOUND_PROFILES = SUPPORTED_SOUND_MODES + setOf(PROFILE_NORMAL, PROFILE_CUSTOM)

        const val DAY_MONDAY = 1
        const val DAY_TUESDAY = 2
        const val DAY_WEDNESDAY = 3
        const val DAY_THURSDAY = 4
        const val DAY_FRIDAY = 5
        const val DAY_SATURDAY = 6
        const val DAY_SUNDAY = 7

        fun parseDays(days: String?): Set<Int> {
            if (days.isNullOrBlank()) return emptySet()
            return days.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        }

        fun formatDays(days: Set<Int>): String {
            return days.sorted().joinToString(",")
        }
    }
}
