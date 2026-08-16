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
import com.soundscheduler.app.data.AutomationStateRepository
import com.soundscheduler.app.data.ExecutionHistoryRepository
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.data.RoutineExecution
import com.soundscheduler.app.data.RoutineDao
import com.soundscheduler.app.utils.LocationRoutineManager
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.RoutineAlarmScheduler
import com.soundscheduler.app.utils.SoundModeController
import java.util.concurrent.Executors

/**
 * Keeps user-requested sound automation visibly armed while enabled routines exist.
 *
 * The service is started from the visible activity. Android 17 may silently ignore ringer-mode
 * writes from a service first created by a background alarm or geofence, so receivers may dispatch
 * work only to an already foreground-ready service. They never create foreground eligibility.
 */
class SoundModeExecutionService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_AUTOMATION -> {
                return if (ensureForegroundAutomation()) START_STICKY else START_NOT_STICKY
            }

            ACTION_STOP_AUTOMATION -> {
                stopForegroundAutomation()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_EXECUTE_ROUTINE -> {
                val routineId = intent.getIntExtra(EXTRA_ROUTINE_ID, INVALID_ROUTINE_ID)
                val sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE)
                    ?.takeIf { it in setOf(Routine.TYPE_TIME, Routine.TYPE_LOCATION, Routine.TYPE_CHARGING) }
                val expectedLocationTransition = intent.getStringExtra(EXTRA_LOCATION_TRANSITION)
                if (routineId <= 0 || sourceType == null || !foregroundAutomationActive) {
                    return START_NOT_STICKY
                }

                executionExecutor.execute {
                    executeRoutine(routineId, sourceType, expectedLocationTransition)
                }
                return START_STICKY
            }

            else -> return START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        val wasForegroundActive = foregroundAutomationActive
        foregroundAutomationActive = false
        if (wasForegroundActive) AutomationStateRepository.markRearmRequired(this)
        super.onDestroy()
    }

    private fun executeRoutine(
        routineId: Int,
        sourceType: String,
        expectedLocationTransition: String?
    ) {
        val database = AppDatabase.getDatabase(this)
        val routineDao = database.routineDao()
        val routine = routineDao.getRoutineById(routineId) ?: return
        val triggerType = triggerTypeFor(sourceType, expectedLocationTransition)

        // Check global pause state
        val state = database.automationStateDao().getState()
        if (state?.isPaused == true) {
            val until = state.pauseUntilMillis
            if (until == null || System.currentTimeMillis() < until) {
                // Still paused
                ExecutionHistoryRepository.recordForRoutineNow(
                    context = this,
                    routine = routine,
                    triggerType = triggerType,
                    outcomeCode = RoutineExecution.OUTCOME_PAUSED,
                    detailCode = if (until != null) "temporary_pause" else "global_pause"
                )
                if (routine.recurrence != null && routine.type == Routine.TYPE_TIME) {
                    RoutineAlarmScheduler.schedule(this, routine)
                }
                return
            }
        }

        if (!isEligible(routine, sourceType, expectedLocationTransition)) return

        val triggerTypeActual = triggerType // triggerTypeFor(sourceType, expectedLocationTransition)
        when (SoundModeController.applyRoutineModeAndConfirm(this, routine)) {
            SoundModeController.ApplyResult.APPLIED -> {
                ExecutionHistoryRepository.recordForRoutineNow(
                    context = this,
                    routine = routine,
                    triggerType = triggerType,
                    outcomeCode = RoutineExecution.OUTCOME_APPLIED,
                    observedMode = routine.targetSoundMode()
                )
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
                ExecutionHistoryRepository.recordForRoutineNow(
                    context = this,
                    routine = routine,
                    triggerType = triggerType,
                    outcomeCode = RoutineExecution.OUTCOME_ACCESS_REQUIRED,
                    detailCode = "modes_access_required"
                )
                NotificationUtils.sendNotification(
                    context = this,
                    title = getString(R.string.sound_access_required_notification_title),
                    message = getString(R.string.sound_access_required_notification_message)
                )
            }

            SoundModeController.ApplyResult.REJECTED_BY_SYSTEM -> {
                ExecutionHistoryRepository.recordForRoutineNow(
                    context = this,
                    routine = routine,
                    triggerType = triggerType,
                    outcomeCode = RoutineExecution.OUTCOME_MODE_REJECTED,
                    observedMode = SoundModeController.currentMode(this),
                    detailCode = "mode_not_retained"
                )
                NotificationUtils.sendNotification(
                    context = this,
                    title = getString(R.string.sound_mode_not_changed_notification_title),
                    message = getString(R.string.sound_mode_not_changed_notification_message)
                )
            }
        }
    }

    private fun triggerTypeFor(sourceType: String, expectedLocationTransition: String?): String {
        return when (sourceType) {
            Routine.TYPE_TIME -> RoutineExecution.TRIGGER_TIME
            Routine.TYPE_LOCATION -> when (expectedLocationTransition) {
                Routine.LOCATION_TRANSITION_EXIT -> RoutineExecution.TRIGGER_LOCATION_EXIT
                else -> RoutineExecution.TRIGGER_LOCATION_ENTER
            }
            Routine.TYPE_CHARGING -> when (expectedLocationTransition) {
                Routine.CHARGING_TRANSITION_DISCONNECTED -> RoutineExecution.TRIGGER_CHARGING_DISCONNECTED
                else -> RoutineExecution.TRIGGER_CHARGING_CONNECTED
            }
            else -> RoutineExecution.TRIGGER_TIME
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
            Routine.TYPE_CHARGING -> {
                expectedLocationTransition in Routine.SUPPORTED_CHARGING_TRANSITIONS &&
                    routine.chargingTransition == expectedLocationTransition
            }
            else -> false
        }
    }

    private fun completeOrReschedule(routine: Routine, routineDao: RoutineDao) {
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
        stopIfNoEnabledRoutinesRemain(routineDao)
    }

    private fun stopIfNoEnabledRoutinesRemain(routineDao: RoutineDao) {
        val hasEnabledTimeRoutine = routineDao.getActiveRoutinesByType(Routine.TYPE_TIME).isNotEmpty()
        val hasEnabledLocationRoutine = routineDao.getActiveRoutinesByType(Routine.TYPE_LOCATION).isNotEmpty()
        if (!hasEnabledTimeRoutine && !hasEnabledLocationRoutine) {
            stopForegroundAutomation()
            stopSelf()
        }
    }

    private fun ensureForegroundAutomation(): Boolean {
        if (foregroundAutomationActive) return true
        return try {
            val notification = NotificationUtils.createAutomationForegroundNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }
            foregroundAutomationActive = true
            AutomationStateRepository.markActive(this)
            true
        } catch (_: RuntimeException) {
            foregroundAutomationActive = false
            AutomationStateRepository.markRearmRequired(this)
            false
        }
    }

    private fun stopForegroundAutomation() {
        if (!foregroundAutomationActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foregroundAutomationActive = false
        AutomationStateRepository.markOff(this)
    }

    private fun soundModeLabel(mode: String): String = when (mode) {
        Routine.PROFILE_SILENT -> getString(R.string.sound_mode_silent)
        Routine.PROFILE_VIBRATE -> getString(R.string.sound_mode_vibrate)
        else -> getString(R.string.sound_mode_ring)
    }

    companion object {
        private const val INVALID_ROUTINE_ID = -1
        private const val FOREGROUND_NOTIFICATION_ID = 91_408
        private const val ACTION_START_AUTOMATION =
            "com.soundscheduler.app.action.START_SOUND_AUTOMATION"
        private const val ACTION_STOP_AUTOMATION =
            "com.soundscheduler.app.action.STOP_SOUND_AUTOMATION"
        private const val ACTION_EXECUTE_ROUTINE =
            "com.soundscheduler.app.action.EXECUTE_SOUND_ROUTINE"
        private const val EXTRA_ROUTINE_ID = "routine_id"
        private const val EXTRA_SOURCE_TYPE = "source_type"
        private const val EXTRA_LOCATION_TRANSITION = "location_transition"
        private val executionExecutor = Executors.newSingleThreadExecutor()

        @Volatile private var foregroundAutomationActive = false

        fun isForegroundReady(): Boolean = foregroundAutomationActive

        /** Call only from a visible activity after the enabled-routine set changes. */
        fun syncAutomationLifecycle(context: Context, hasEnabledRoutines: Boolean) {
            val intent = Intent(
                context,
                SoundModeExecutionService::class.java
            ).setAction(if (hasEnabledRoutines) ACTION_START_AUTOMATION else ACTION_STOP_AUTOMATION)
            try {
                if (hasEnabledRoutines) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: RuntimeException) {
                // The home screen will expose re-arm status once execution history is available.
            }
        }

        /**
         * Queues a time routine only when the user-visible automation service is already ready.
         * Returns false when the user must reopen Sound Scheduler to re-arm automation.
         */
        fun startForTimeRoutine(context: Context, routineId: Int): Boolean {
            return startRoutineExecution(context, routineId, Routine.TYPE_TIME)
        }

        /**
         * Queues a place routine only when the user-visible automation service is already ready.
         * Returns false when the user must reopen Sound Scheduler to re-arm automation.
         */
        fun startForLocationRoutine(context: Context, routineId: Int, transition: String): Boolean {
            return startRoutineExecution(context, routineId, Routine.TYPE_LOCATION, transition)
        }

        fun startForChargingRoutine(context: Context, routineId: Int, transition: String): Boolean {
            return startRoutineExecution(context, routineId, Routine.TYPE_CHARGING, transition)
        }

        private fun startRoutineExecution(
            context: Context,
            routineId: Int,
            sourceType: String,
            locationTransition: String? = null
        ): Boolean {
            if (routineId <= 0 || !foregroundAutomationActive) return false
            val intent = Intent(context, SoundModeExecutionService::class.java).apply {
                action = ACTION_EXECUTE_ROUTINE
                putExtra(EXTRA_ROUTINE_ID, routineId)
                putExtra(EXTRA_SOURCE_TYPE, sourceType)
                putExtra(EXTRA_LOCATION_TRANSITION, locationTransition)
            }
            return try {
                context.startService(intent) != null
            } catch (_: RuntimeException) {
                false
            }
        }
    }
}
