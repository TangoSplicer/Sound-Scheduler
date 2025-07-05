package com.soundscheduler.app.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract

object CalendarRoutineManager {

    fun checkAndTriggerCalendarRoutines(context: Context) {
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
        )

        val now = System.currentTimeMillis()
        val end = now + (60 * 60 * 1000) // 1 hour ahead

        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(now.toString(), end.toString())

        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(0)

                NotificationUtils.sendNotification(
                    context,
                    "Upcoming Event",
                    "Event \"$title\" starting soon."
                )
                UsageAnalyticsManager.incrementNotificationsSent(context)
            }
        }
    }
}