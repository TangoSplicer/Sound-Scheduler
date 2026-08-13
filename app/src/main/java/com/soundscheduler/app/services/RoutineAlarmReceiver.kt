package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soundscheduler.app.R
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.RoutineAlarmScheduler
import com.soundscheduler.app.utils.RoutineRescheduler
import com.soundscheduler.app.utils.SoundModeController

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RoutineAlarmScheduler.ACTION_TRIGGER_ROUTINE) return

        val routineId = intent.getIntExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_ID, -1)
        val routineTime = intent.getLongExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_TIME, 0L)
        if (routineId <= 0 || routineTime <= 0L) return

        val title = intent.getStringExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)
        val recurrence = intent.getStringExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_RECURRENCE)
        val targetMode = intent.getStringExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_SOUND_MODE)
            ?.takeIf { it in Routine.SUPPORTED_SOUND_MODES }
            ?: Routine.PROFILE_RING
        val routine = Routine(
            id = routineId,
            title = title,
            type = Routine.TYPE_TIME,
            time = routineTime,
            recurrence = recurrence,
            soundProfile = targetMode
        )

        when (SoundModeController.applyRoutineMode(context, routine)) {
            SoundModeController.ApplyResult.APPLIED -> {
                NotificationUtils.sendNotification(
                    context = context,
                    title = context.getString(R.string.sound_mode_changed_notification_title),
                    message = context.getString(
                        R.string.sound_mode_changed_notification_message,
                        title,
                        soundModeLabel(context, targetMode)
                    )
                )
                if (recurrence == null) {
                    RoutineRescheduler.markOneTimeRoutineCompleted(context, routineId)
                } else {
                    RoutineAlarmScheduler.schedule(context, routine)
                }
            }

            SoundModeController.ApplyResult.POLICY_ACCESS_REQUIRED -> {
                NotificationUtils.sendNotification(
                    context = context,
                    title = context.getString(R.string.sound_access_required_notification_title),
                    message = context.getString(R.string.sound_access_required_notification_message)
                )
                if (recurrence != null) RoutineAlarmScheduler.schedule(context, routine)
            }

            SoundModeController.ApplyResult.REJECTED_BY_SYSTEM -> {
                NotificationUtils.sendNotification(
                    context = context,
                    title = context.getString(R.string.sound_mode_not_changed_notification_title),
                    message = context.getString(R.string.sound_mode_not_changed_notification_message)
                )
                if (recurrence != null) RoutineAlarmScheduler.schedule(context, routine)
            }
        }
    }

    private fun soundModeLabel(context: Context, targetMode: String): String = when (targetMode) {
        Routine.PROFILE_SILENT -> context.getString(R.string.sound_mode_silent)
        Routine.PROFILE_VIBRATE -> context.getString(R.string.sound_mode_vibrate)
        else -> context.getString(R.string.sound_mode_ring)
    }
}
