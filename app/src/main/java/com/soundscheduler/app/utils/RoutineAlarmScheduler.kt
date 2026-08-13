package com.soundscheduler.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.services.RoutineAlarmReceiver
import java.util.Calendar

object RoutineAlarmScheduler {
    const val ACTION_TRIGGER_ROUTINE = "com.soundscheduler.app.action.TRIGGER_ROUTINE"
    const val EXTRA_ROUTINE_ID = "routine_id"
    const val EXTRA_ROUTINE_TITLE = "routine_title"
    const val EXTRA_ROUTINE_RECURRENCE = "routine_recurrence"
    const val EXTRA_ROUTINE_TIME = "routine_time"

    data class ScheduleResult(
        val triggerAtMillis: Long,
        val exact: Boolean
    )

    fun schedule(context: Context, routine: Routine): ScheduleResult? {
        if (routine.type != Routine.TYPE_TIME || routine.isCompleted || routine.id <= 0) return null

        val triggerAtMillis = nextTriggerAt(routine, System.currentTimeMillis()) ?: run {
            cancel(context, routine)
            return null
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, routine)
        val canUseExactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        if (canUseExactAlarm) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        return ScheduleResult(triggerAtMillis = triggerAtMillis, exact = canUseExactAlarm)
    }

    fun cancel(context: Context, routine: Routine) {
        if (routine.id <= 0) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, routine))
    }

    fun scheduleAll(context: Context, routines: Iterable<Routine>) {
        routines.forEach { schedule(context, it) }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun nextTriggerAt(routine: Routine, nowMillis: Long): Long? {
        val scheduledAt = routine.time ?: return null
        if (scheduledAt > nowMillis) return scheduledAt
        val recurrence = routine.recurrence ?: return null
        val calendar = Calendar.getInstance().apply { timeInMillis = scheduledAt }

        do {
            when (recurrence) {
                Routine.RECURRENCE_DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                Routine.RECURRENCE_WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                Routine.RECURRENCE_MONTHLY -> calendar.add(Calendar.MONTH, 1)
                else -> return null
            }
        } while (calendar.timeInMillis <= nowMillis)

        return calendar.timeInMillis
    }

    private fun pendingIntent(context: Context, routine: Routine): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ROUTINE
            putExtra(EXTRA_ROUTINE_ID, routine.id)
            putExtra(EXTRA_ROUTINE_TITLE, routine.title)
            putExtra(EXTRA_ROUTINE_RECURRENCE, routine.recurrence)
            putExtra(EXTRA_ROUTINE_TIME, routine.time ?: 0L)
        }
        return PendingIntent.getBroadcast(
            context,
            routine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
