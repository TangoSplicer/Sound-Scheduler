@file:Suppress(
    "unused",                        // functions called reflectively or from XML/Java
    "RedundantSamConstructor",       // we’ll use lambdas instead
    "RedundantExplicitTypeArguments",// lambdas infer types
    "UNUSED_PARAMETER"               // context parameter in remove is used only for suppression
)
package com.soundscheduler.app.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.soundscheduler.app.services.GeofenceReceiver

/**
 * Singleton manager for adding/removing location‐based routines.
 *
 * 1) Call initialize(context) once (e.g. in Application.onCreate).
 * 2) Then use addLocationRoutine(...) / removeLocationRoutine(...).
 */
object LocationRoutineManager {

    private lateinit var geofencingClient: GeofencingClient

    /** Must be called once before any add/remove operations. */
    fun initialize(context: Context) {
        geofencingClient = LocationServices.getGeofencingClient(context)
    }

    @SuppressLint("MissingPermission")
    fun addLocationRoutine(
        context: Context,
        routineId: String,
        latitude: Double,
        longitude: Double,
        radius: Float
    ) {
        // 1) Build the Geofence
        val geofence = Geofence.Builder()
            .setRequestId(routineId)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        // 2) Wrap it in a request
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // 3) Prepare the PendingIntent
        val intent = Intent(context, GeofenceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routineId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4) Add to the GeofencingClient, using lambdas
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                // Optional: handle successful addition
            }
            .addOnFailureListener { e ->
                // Optional: handle failure
            }
    }

    fun removeLocationRoutine(context: Context, routineId: String) {
        // We don’t actually need the context here, but it's kept for signature consistency
        geofencingClient.removeGeofences(listOf(routineId))
            .addOnSuccessListener {
                // Optional: handle successful removal
            }
            .addOnFailureListener { e ->
                // Optional: handle failure
            }
    }
}
