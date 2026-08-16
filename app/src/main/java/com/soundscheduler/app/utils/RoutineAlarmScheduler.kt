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
    const val EXTRA_ROUTINE_SOUND_MODE = "routine_sound_mode"

    data class ScheduleResult(
        val triggerAtMillis: Long,
        val exact: Boolean
    )

    fun schedule(context: Context, routine: Routine): ScheduleResult? {
        if (routine.type != Routine.TYPE_TIME || routine.isCompleted || !routine.isEnabled || routine.id <= 0) return null

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
        val recurrence = routine.recurrence
        val allowedDays = Routine.parseDays(routine.daysOfWeek)
        val calendar = Calendar.getInstance().apply { timeInMillis = scheduledAt }

        // If it's a one-time routine (no recurrence), just check if it's in the future
        if (recurrence == null) {
            return if (scheduledAt > nowMillis) scheduledAt else null
        }

        // For recurring routines, we need to find the first occurrence >= scheduledAt AND > nowMillis
        // that also matches the allowed days if specified.
        
        // If scheduledAt is already in the future, start checking from there.
        // If not, we'll increment based on recurrence rules.
        
        while (calendar.timeInMillis <= nowMillis || 
               (recurrence == Routine.RECURRENCE_WEEKLY && allowedDays.isNotEmpty() && 
                !allowedDays.contains(calendarDayToRoutineDay(calendar.get(Calendar.DAY_OF_WEEK))))) {
            
            when (recurrence) {
                Routine.RECURRENCE_DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                Routine.RECURRENCE_WEEKLY -> {
                    // If specific days are selected, we increment day by day.
                    // Otherwise, we increment by a full week.
                    if (allowedDays.isNotEmpty()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } else {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                Routine.RECURRENCE_MONTHLY -> calendar.add(Calendar.MONTH, 1)
                else -> return null
            }
            
            // Safety break to prevent infinite loops if something is wrong
            if (calendar.timeInMillis > nowMillis + 366L * 24 * 60 * 60 * 1000) break
        }

        return calendar.timeInMillis
    }

    private fun calendarDayToRoutineDay(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY -> Routine.DAY_MONDAY
        Calendar.TUESDAY -> Routine.DAY_TUESDAY
        Calendar.WEDNESDAY -> Routine.DAY_WEDNESDAY
        Calendar.THURSDAY -> Routine.DAY_THURSDAY
        Calendar.FRIDAY -> Routine.DAY_FRIDAY
        Calendar.SATURDAY -> Routine.DAY_SATURDAY
        Calendar.SUNDAY -> Routine.DAY_SUNDAY
        else -> 0
    }

    private fun pendingIntent(context: Context, routine: Routine): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ROUTINE
            putExtra(EXTRA_ROUTINE_ID, routine.id)
            putExtra(EXTRA_ROUTINE_TITLE, routine.title)
            putExtra(EXTRA_ROUTINE_RECURRENCE, routine.recurrence)
            putExtra(EXTRA_ROUTINE_TIME, routine.time ?: 0L)
            putExtra(EXTRA_ROUTINE_SOUND_MODE, routine.targetSoundMode())
        }
        return PendingIntent.getBroadcast(
            context,
            routine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
