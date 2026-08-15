package com.soundscheduler.app.data

import android.content.Context
import com.soundscheduler.app.utils.LocationRoutineManager
import com.soundscheduler.app.utils.RoutineAlarmScheduler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Coordinates an explicit global pause without losing the user’s enabled-routine choices. */
object AutomationControlRepository {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun pauseAll(context: Context, onPaused: () -> Unit = {}) {
        executor.execute {
            val database = AppDatabase.getDatabase(context)
            val routineDao = database.routineDao()
            val activeTime = routineDao.getActiveRoutinesByType(Routine.TYPE_TIME)
            val activeLocation = routineDao.getActiveRoutinesByType(Routine.TYPE_LOCATION)

            activeTime.forEach { RoutineAlarmScheduler.cancel(context, it) }
            activeLocation.forEach { LocationRoutineManager.removeGeofence(context, it) }
            database.runInTransaction {
                routineDao.pauseAllEnabledRoutines()
                val stateDao = database.automationStateDao()
                val state = stateDao.getState() ?: AutomationState()
                stateDao.saveState(
                    state.copy(
                        isPaused = true,
                        lastStateCode = AutomationState.STATE_PAUSED,
                        lastStateDetailCode = null
                    )
                )
            }
            onPaused()
        }
    }

    fun resumeAll(context: Context, onResumed: () -> Unit = {}) {
        executor.execute {
            val database = AppDatabase.getDatabase(context)
            val routineDao = database.routineDao()
            database.runInTransaction {
                routineDao.resumeAllPreviouslyEnabledRoutines()
                val stateDao = database.automationStateDao()
                val state = stateDao.getState() ?: AutomationState()
                stateDao.saveState(
                    state.copy(
                        isPaused = false,
                        lastStateCode = AutomationState.STATE_OFF,
                        lastStateDetailCode = null
                    )
                )
            }

            routineDao.getActiveRoutinesByType(Routine.TYPE_TIME)
                .forEach { RoutineAlarmScheduler.schedule(context, it) }
            LocationRoutineManager.refreshActiveGeofences(context)
            onResumed()
        }
    }
}
