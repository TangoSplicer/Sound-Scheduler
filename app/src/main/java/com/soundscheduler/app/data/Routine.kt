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
)