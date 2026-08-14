package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soundscheduler.app.utils.RoutineAlarmScheduler

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RoutineAlarmScheduler.ACTION_TRIGGER_ROUTINE) return

        val routineId = intent.getIntExtra(RoutineAlarmScheduler.EXTRA_ROUTINE_ID, INVALID_ROUTINE_ID)
        if (routineId <= 0) return

        SoundModeExecutionService.startForTimeRoutine(context, routineId)
    }

    private companion object {
        const val INVALID_ROUTINE_ID = -1
    }
}
