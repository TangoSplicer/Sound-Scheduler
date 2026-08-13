package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.RoutineAlarmScheduler
import com.soundscheduler.app.utils.RoutineRescheduler

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RoutineAlarmScheduler.ACTION_TRIGGER_ROUTINE) return

        val routineId = intent.getIntExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_ID, -1)
        val title = intent.getStringExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(com.soundscheduler.app.R.string.app_name)
        val recurrence = intent.getStringExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_RECURRENCE)
        val routineTime = intent.getLongExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_TIME, 0L)

        NotificationUtils.sendRoutineNotification(context, routineId, title)

        if (recurrence == null) {
            RoutineRescheduler.markOneTimeRoutineCompleted(context, routineId)
        } else {
            RoutineAlarmScheduler.schedule(
                context,
                Routine(
                    id = routineId,
                    title = title,
                    type = Routine.TYPE_TIME,
                    time = routineTime,
                    recurrence = recurrence
                )
            )
        }
    }
}
