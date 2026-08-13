package com.soundscheduler.app.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.services.LocationRoutineReceiver
import java.util.concurrent.Executors

object LocationRoutineManager {
    const val MAX_ACTIVE_LOCATION_ROUTINES = 50
    private const val GEOFENCE_REQUEST_PREFIX = "sound_location_"
    private const val DEFAULT_RESPONSIVENESS_MILLIS = 5 * 60 * 1000

    private val databaseExecutor = Executors.newSingleThreadExecutor()

    fun hasForegroundLocationAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBackgroundLocationAccess(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasRequiredLocationAccess(context: Context): Boolean =
        hasForegroundLocationAccess(context) && hasBackgroundLocationAccess(context)

    fun captureCurrentLocation(
        context: Context,
        onResult: (Location?) -> Unit
    ) {
        if (!hasForegroundLocationAccess(context)) {
            onResult(null)
            return
        }

        val tokenSource = CancellationTokenSource()
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                .addOnSuccessListener(onResult)
                .addOnFailureListener { onResult(null) }
        } catch (_: SecurityException) {
            onResult(null)
        }
    }

    fun refreshActiveGeofences(context: Context, onComplete: (Boolean) -> Unit = {}) {
        if (!hasRequiredLocationAccess(context)) {
            onComplete(false)
            return
        }

        databaseExecutor.execute {
            val routines = AppDatabase.getDatabase(context).routineDao()
                .getActiveRoutinesByType(Routine.TYPE_LOCATION)
                .filter { it.hasUsableLocation() }
                .take(MAX_ACTIVE_LOCATION_ROUTINES)
            val client = LocationServices.getGeofencingClient(context)
            val pendingIntent = geofencePendingIntent(context)

            try {
                client.removeGeofences(pendingIntent)
                    .addOnCompleteListener {
                        if (routines.isEmpty()) {
                            onComplete(true)
                        } else {
                            client.addGeofences(buildRequest(routines), pendingIntent)
                                .addOnSuccessListener { onComplete(true) }
                                .addOnFailureListener { onComplete(false) }
                        }
                    }
            } catch (_: SecurityException) {
                onComplete(false)
            }
        }
    }

    fun removeGeofence(context: Context, routine: Routine) {
        if (routine.id <= 0) return
        try {
            LocationServices.getGeofencingClient(context)
                .removeGeofences(listOf(requestIdFor(routine.id)))
        } catch (_: SecurityException) {
            // The fence may already be unavailable after permission removal or a system reset.
        }
    }

    fun requestIdFor(routineId: Int): String = "$GEOFENCE_REQUEST_PREFIX$routineId"

    fun routineIdFromRequestId(requestId: String): Int? =
        requestId.removePrefix(GEOFENCE_REQUEST_PREFIX).toIntOrNull()
            ?.takeIf { requestId == requestIdFor(it) }

    private fun buildRequest(routines: List<Routine>): GeofencingRequest =
        GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(routines.map(::buildGeofence))
            .build()

    private fun buildGeofence(routine: Routine): Geofence =
        Geofence.Builder()
            .setRequestId(requestIdFor(routine.id))
            .setCircularRegion(
                routine.latitude!!,
                routine.longitude!!,
                routine.radiusMeters!!.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setNotificationResponsiveness(DEFAULT_RESPONSIVENESS_MILLIS)
            .setTransitionTypes(
                if (routine.locationTransition == Routine.LOCATION_TRANSITION_EXIT) {
                    Geofence.GEOFENCE_TRANSITION_EXIT
                } else {
                    Geofence.GEOFENCE_TRANSITION_ENTER
                }
            )
            .build()

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LocationRoutineReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, 91_407, intent, flags)
    }
}
