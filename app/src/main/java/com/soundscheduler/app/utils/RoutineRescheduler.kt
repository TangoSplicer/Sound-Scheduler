package com.soundscheduler.app.utils

import android.content.Context
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.Routine
import java.util.concurrent.Executors

object RoutineRescheduler {
    private val executor = Executors.newSingleThreadExecutor()

    fun rescheduleActiveTimeRoutines(context: Context) {
        val appContext = context.applicationContext
        executor.execute {
            if (!SoundModeController.hasNotificationPolicyAccess(appContext)) return@execute

            val routines = AppDatabase.getDatabase(appContext)
                .routineDao()
                .getActiveRoutinesByType(Routine.TYPE_TIME)
            routines.forEach { routine ->
                RoutineAlarmScheduler.schedule(appContext, routine)
            }
        }
    }

    fun markOneTimeRoutineCompleted(context: Context, routineId: Int) {
        if (routineId <= 0) return
        val appContext = context.applicationContext
        executor.execute {
            AppDatabase.getDatabase(appContext).routineDao().markCompleted(routineId)
        }
    }
}
