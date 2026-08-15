package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soundscheduler.app.R
import com.soundscheduler.app.data.ExecutionHistoryRepository
import com.soundscheduler.app.data.RoutineExecution
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.RoutineAlarmScheduler

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RoutineAlarmScheduler.ACTION_TRIGGER_ROUTINE) return

        val routineId = intent.getIntExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_ID, INVALID_ROUTINE_ID)
        if (routineId <= 0) return

        if (!SoundModeExecutionService.startForTimeRoutine(context, routineId)) {
            ExecutionHistoryRepository.recordAutomationRearmRequired(
                context = context,
                routineId = routineId,
                triggerType = RoutineExecution.TRIGGER_TIME
            )
            NotificationUtils.sendNotification(
                context,
                context.getString(R.string.automation_rearm_notification_title),
                context.getString(R.string.automation_rearm_notification_message)
            )
        }
    }

    private companion object {
        const val INVALID_ROUTINE_ID = -1
    }
}
