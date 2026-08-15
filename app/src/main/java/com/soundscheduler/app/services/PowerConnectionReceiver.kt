package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soundscheduler.app.R
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.ExecutionHistoryRepository
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.data.RoutineExecution
import com.soundscheduler.app.utils.NotificationUtils
import java.util.concurrent.Executors

/** Receives Android power connection changes for explicitly configured, local charging routines. */
class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val transition = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> Routine.CHARGING_TRANSITION_CONNECTED
            Intent.ACTION_POWER_DISCONNECTED -> Routine.CHARGING_TRANSITION_DISCONNECTED
            else -> return
        }
        val pendingResult = goAsync()
        receiverExecutor.execute {
            try {
                val routines = AppDatabase.getDatabase(context).routineDao()
                    .getActiveRoutinesByType(Routine.TYPE_CHARGING)
                    .filter { it.chargingTransition == transition }
                var needsRearmNotification = false
                routines.forEach { routine ->
                    if (!SoundModeExecutionService.startForChargingRoutine(context, routine.id, transition)) {
                        ExecutionHistoryRepository.recordAutomationRearmRequired(
                            context = context,
                            routineId = routine.id,
                            triggerType = if (transition == Routine.CHARGING_TRANSITION_DISCONNECTED) {
                                RoutineExecution.TRIGGER_CHARGING_DISCONNECTED
                            } else {
                                RoutineExecution.TRIGGER_CHARGING_CONNECTED
                            }
                        )
                        needsRearmNotification = true
                    }
                }
                if (needsRearmNotification) {
                    NotificationUtils.sendNotification(
                        context,
                        context.getString(R.string.automation_rearm_notification_title),
                        context.getString(R.string.automation_rearm_notification_message)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val receiverExecutor = Executors.newSingleThreadExecutor()
    }
}
