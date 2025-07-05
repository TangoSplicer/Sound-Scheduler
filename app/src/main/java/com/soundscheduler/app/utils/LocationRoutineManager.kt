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

object LocationRoutineManager {

    private lateinit var geofencingClient: GeofencingClient

    fun initialize(context: Context) {
        geofencingClient = LocationServices.getGeofencingClient(context)
    }

    @SuppressLint("MissingPermission")
    fun addLocationRoutine(context: Context, routineId: String, latitude: Double, longitude: Double, radius: Float) {
        val geofence = Geofence.Builder()
            .setRequestId(routineId)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routineId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
    }

    fun removeLocationRoutine(context: Context, routineId: String) {
        geofencingClient.removeGeofences(listOf(routineId))
    }
}