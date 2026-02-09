@file:Suppress(
    "unused",
    "RedundantSamConstructor",
    "RedundantExplicitTypeArguments",
    "UNUSED_PARAMETER"
)
package com.soundscheduler.app.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.soundscheduler.app.services.GeofenceReceiver

object LocationRoutineManager {
    
    private const val TAG = "LocationRoutineManager"
    private lateinit var geofencingClient: GeofencingClient

    fun initialize(context: Context) {
        try {
            geofencingClient = LocationServices.getGeofencingClient(context)
            Log.d(TAG, "GeofencingClient initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GeofencingClient", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun addLocationRoutine(
        context: Context,
        routineId: String,
        latitude: Double,
        longitude: Double,
        radius: Float
    ) {
        try {
            require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
            require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
            require(radius in 1f..100000f) { "Radius must be between 1 and 100000 meters" }
            require(routineId.isNotBlank()) { "Routine ID cannot be blank" }
            
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                Log.e(TAG, "Location permission not granted for routine: $routineId")
                return
            }
            
            val geofence = Geofence.Builder()
                .setRequestId(routineId)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT
                )
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
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added successfully for routine: $routineId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add geofence for routine: $routineId", e)
                }
                
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid parameters for geofence: $routineId", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error adding geofence: $routineId", e)
        }
    }

    fun removeLocationRoutine(context: Context, routineId: String) {
        try {
            require(routineId.isNotBlank()) { "Routine ID cannot be blank" }
            
            geofencingClient.removeGeofences(listOf(routineId))
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence removed successfully for routine: $routineId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to remove geofence for routine: $routineId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofence: $routineId", e)
        }
    }
}
