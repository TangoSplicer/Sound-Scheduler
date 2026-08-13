package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.soundscheduler.app.R
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.utils.LocationRoutineManager
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.SoundModeController
import java.util.concurrent.Executors

class LocationRoutineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        val expectedTransition = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> Routine.LOCATION_TRANSITION_ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> Routine.LOCATION_TRANSITION_EXIT
            else -> return
        }
        val routineIds = event.triggeringGeofences.orEmpty()
            .mapNotNull { LocationRoutineManager.routineIdFromRequestId(it.requestId) }
            .distinct()
        if (routineIds.isEmpty()) return

        val pendingResult = goAsync()
        transitionExecutor.execute {
            try {
                val routineDao = AppDatabase.getDatabase(context).routineDao()
                routineIds.forEach { routineId ->
                    val routine = routineDao.getRoutineById(routineId) ?: return@forEach
                    if (
                        !routine.isEnabled ||
                        routine.isCompleted ||
                        !routine.hasUsableLocation() ||
                        routine.locationTransition != expectedTransition
                    ) {
                        return@forEach
                    }

                    when (SoundModeController.applyRoutineMode(context, routine)) {
                        SoundModeController.ApplyResult.APPLIED -> {
                            NotificationUtils.sendNotification(
                                context = context,
                                title = context.getString(R.string.sound_mode_changed_notification_title),
                                message = context.getString(
                                    R.string.sound_mode_changed_notification_message,
                                    routine.title,
                                    soundModeLabel(context, routine.targetSoundMode())
                                )
                            )
                            if (routine.recurrence == null) {
                                routineDao.markCompleted(routine.id)
                                LocationRoutineManager.removeGeofence(context, routine)
                            }
                        }

                        SoundModeController.ApplyResult.POLICY_ACCESS_REQUIRED -> {
                            NotificationUtils.sendNotification(
                                context = context,
                                title = context.getString(R.string.sound_access_required_notification_title),
                                message = context.getString(R.string.sound_access_required_notification_message)
                            )
                        }

                        SoundModeController.ApplyResult.REJECTED_BY_SYSTEM -> {
                            NotificationUtils.sendNotification(
                                context = context,
                                title = context.getString(R.string.sound_mode_not_changed_notification_title),
                                message = context.getString(R.string.sound_mode_not_changed_notification_message)
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun soundModeLabel(context: Context, targetMode: String): String = when (targetMode) {
        Routine.PROFILE_SILENT -> context.getString(R.string.sound_mode_silent)
        Routine.PROFILE_VIBRATE -> context.getString(R.string.sound_mode_vibrate)
        else -> context.getString(R.string.sound_mode_ring)
    }

    private companion object {
        val transitionExecutor = Executors.newSingleThreadExecutor()
    }
}
