package com.soundscheduler.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.soundscheduler.app.R
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.utils.LocationRoutineManager
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.RoutineAlarmScheduler
import com.soundscheduler.app.utils.SoundModeController
import java.util.concurrent.Executors

class SoundModeExecutionService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val routineId = intent?.getIntExtra(EXTRA_ROUTINE_ID, INVALID_ROUTINE_ID) ?: INVALID_ROUTINE_ID
        val sourceType = intent?.getStringExtra(EXTRA_SOURCE_TYPE)
            ?.takeIf { it in setOf(Routine.TYPE_TIME, Routine.TYPE_LOCATION) }
        val expectedLocationTransition = intent?.getStringExtra(EXTRA_LOCATION_TRANSITION)
        if (routineId <= 0 || sourceType == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startExecutionForeground()
        executionExecutor.execute {
            try {
                executeRoutine(routineId, sourceType, expectedLocationTransition)
            } finally {
                stopExecutionForeground()
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun executeRoutine(
        routineId: Int,
        sourceType: String,
        expectedLocationTransition: String?
    ) {
        val routineDao = AppDatabase.getDatabase(this).routineDao()
        val routine = routineDao.getRoutineById(routineId) ?: return
        if (!isEligible(routine, sourceType, expectedLocationTransition)) return

        when (SoundModeController.applyRoutineModeAndConfirm(this, routine)) {
            SoundModeController.ApplyResult.APPLIED -> {
                NotificationUtils.sendNotification(
                    context = this,
                    title = getString(R.string.sound_mode_changed_notification_title),
                    message = getString(
                        R.string.sound_mode_changed_notification_message,
                        routine.title,
                        soundModeLabel(routine.targetSoundMode())
                    )
                )
                completeOrReschedule(routine, routineDao)
            }

            SoundModeController.ApplyResult.POLICY_ACCESS_REQUIRED -> {
                NotificationUtils.sendNotification(
                    context = this,
                    title = getString(R.string.sound_access_required_notification_title),
                    message = getString(R.string.sound_access_required_notification_message)
                )
            }

            SoundModeController.ApplyResult.REJECTED_BY_SYSTEM -> {
                NotificationUtils.sendNotification(
                    context = this,
                    title = getString(R.string.sound_mode_not_changed_notification_title),
                    message = getString(R.string.sound_mode_not_changed_notification_message)
                )
            }
        }
    }

    private fun isEligible(
        routine: Routine,
        sourceType: String,
        expectedLocationTransition: String?
    ): Boolean {
        if (!routine.isEnabled || routine.isCompleted || routine.type != sourceType) return false
        return when (sourceType) {
            Routine.TYPE_TIME -> routine.time != null
            Routine.TYPE_LOCATION -> {
                routine.hasUsableLocation() &&
                    expectedLocationTransition in Routine.SUPPORTED_LOCATION_TRANSITIONS &&
                    routine.locationTransition == expectedLocationTransition
            }
            else -> false
        }
    }

    private fun completeOrReschedule(routine: Routine, routineDao: com.soundscheduler.app.data.RoutineDao) {
        if (routine.recurrence != null) {
            if (routine.type == Routine.TYPE_TIME) {
                RoutineAlarmScheduler.schedule(this, routine)
            }
            return
        }

        routineDao.markCompleted(routine.id)
        if (routine.type == Routine.TYPE_LOCATION) {
            LocationRoutineManager.removeGeofence(this, routine)
        }
    }

    private fun startExecutionForeground() {
        val notification = NotificationUtils.createForegroundExecutionNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun stopExecutionForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun soundModeLabel(mode: String): String = when (mode) {
        Routine.PROFILE_SILENT -> getString(R.string.sound_mode_silent)
        Routine.PROFILE_VIBRATE -> getString(R.string.sound_mode_vibrate)
        else -> getString(R.string.sound_mode_ring)
    }

    companion object {
        private const val INVALID_ROUTINE_ID = -1
        private const val FOREGROUND_NOTIFICATION_ID = 91_408
        private const val EXTRA_ROUTINE_ID = "routine_id"
        private const val EXTRA_SOURCE_TYPE = "source_type"
        private const val EXTRA_LOCATION_TRANSITION = "location_transition"
        private val executionExecutor = Executors.newSingleThreadExecutor()

        fun startForTimeRoutine(context: Context, routineId: Int) {
            start(context, routineId, Routine.TYPE_TIME)
        }

        fun startForLocationRoutine(context: Context, routineId: Int, transition: String) {
            start(context, routineId, Routine.TYPE_LOCATION, transition)
        }

        private fun start(
            context: Context,
            routineId: Int,
            sourceType: String,
            locationTransition: String? = null
        ) {
            if (routineId <= 0) return
            val intent = Intent(context, SoundModeExecutionService::class.java).apply {
                putExtra(EXTRA_ROUTINE_ID, routineId)
                putExtra(EXTRA_SOURCE_TYPE, sourceType)
                putExtra(EXTRA_LOCATION_TRANSITION, locationTransition)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: IllegalStateException) {
                // Android can temporarily reject a background start outside an eligible trigger window.
            }
        }
    }
}
