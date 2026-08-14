package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.utils.LocationRoutineManager

class LocationRoutineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> Routine.LOCATION_TRANSITION_ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> Routine.LOCATION_TRANSITION_EXIT
            else -> return
        }
        event.triggeringGeofences.orEmpty()
            .mapNotNull { LocationRoutineManager.routineIdFromRequestId(it.requestId) }
            .distinct()
            .forEach { routineId ->
                SoundModeExecutionService.startForLocationRoutine(context, routineId, transition)
            }
    }
}
