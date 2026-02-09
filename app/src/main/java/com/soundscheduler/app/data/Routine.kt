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
    val calendarEventId: String? = null,
    val isCompleted: Boolean = false,
    val recurrence: String? = null,
    val soundProfile: String? = "normal"
) {
    init {
        // Input validation
        require(title.isNotBlank()) { "Routine title cannot be blank" }
        require(title.length <= 100) { "Routine title cannot exceed 100 characters" }
        require(type in listOf("time", "location", "calendar")) { 
            "Invalid routine type: $type. Must be 'time', 'location', or 'calendar'" 
        }
        require(soundProfile in listOf("normal", "silent", "vibrate", "custom")) { 
            "Invalid sound profile: $soundProfile" 
        }
        require(recurrence == null || recurrence in listOf("daily", "weekly", "monthly", "custom")) {
            "Invalid recurrence type: $recurrence"
        }
        
        // Type-specific validation
        when (type) {
            "time" -> {
                require(time != null) { "Time-based routine must have a time value" }
                require(time > 0) { "Time value must be positive" }
            }
            "location" -> {
                require(!location.isNullOrBlank()) { "Location-based routine must have a location" }
            }
            "calendar" -> {
                require(!calendarEventId.isNullOrBlank()) { "Calendar-based routine must have an event ID" }
            }
        }
    }
}
