package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.UsageAnalyticsManager

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Null‐check right away
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        // orEmpty() turns a nullable List into an empty one if null
        val triggeringGeofences = geofencingEvent.triggeringGeofences.orEmpty()

        for (geofence in triggeringGeofences) {
            val routineId = geofence.requestId
            val message = when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered location for routine $routineId"
                Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited location for routine $routineId"
                else -> "Location event for routine $routineId"
            }
            NotificationUtils.sendNotification(context, "Location Trigger", message)
            UsageAnalyticsManager.incrementNotificationsSent(context)
        }
    }
}
