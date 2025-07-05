package com.soundscheduler.app.data

enum class RecurrenceType(val displayName: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    override fun toString(): String = displayName
}