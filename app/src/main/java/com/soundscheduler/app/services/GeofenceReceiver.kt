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
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) return

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        for (geofence in triggeringGeofences) {
            val routineId = geofence.requestId
            val message = when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered location for routine $routineId"
                Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited location for routine $routineId"
                else -> "Location event for routine $routineId"
            }
            NotificationUtils.sendNotification(context, "Location Trigger", message)
            UsageAnalyticsManager.incrementNotificationsSent(context)
        }
    }
}