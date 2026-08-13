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
            val routineDao = AppDatabase.getDatabase(appContext).routineDao()
            val routines = routineDao.getActiveRoutinesByType(Routine.TYPE_TIME)
            routines.forEach { routine ->
                val scheduled = RoutineAlarmScheduler.schedule(appContext, routine)
                if (scheduled == null && routine.recurrence == null) {
                    routineDao.markCompleted(routine.id)
                }
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
