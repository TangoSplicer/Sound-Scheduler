package com.soundscheduler.app.data

import android.content.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Local state bridge between the foreground service and user-visible automation controls. */
object AutomationStateRepository {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun ensureState(context: Context) {
        executor.execute {
            val dao = AppDatabase.getDatabase(context).automationStateDao()
            if (dao.getState() == null) dao.saveState(AutomationState())
        }
    }

    fun markActive(context: Context) {
        executor.execute {
            val dao = AppDatabase.getDatabase(context).automationStateDao()
            val now = System.currentTimeMillis()
            val current = dao.getState() ?: AutomationState()
            dao.saveState(
                current.copy(
                    isPaused = false,
                    lastArmedAtMillis = current.lastArmedAtMillis ?: now,
                    lastActiveAtMillis = now,
                    lastStateCode = AutomationState.STATE_ACTIVE,
                    lastStateDetailCode = null
                )
            )
        }
    }

    fun markPaused(context: Context) {
        updateState(context, isPaused = true, stateCode = AutomationState.STATE_PAUSED)
    }

    fun markOff(context: Context) {
        updateState(context, isPaused = false, stateCode = AutomationState.STATE_OFF)
    }

    fun markRearmRequired(context: Context) {
        updateState(
            context = context,
            isPaused = false,
            stateCode = AutomationState.STATE_REARM_REQUIRED,
            detailCode = "foreground_service_not_ready"
        )
    }

    private fun updateState(
        context: Context,
        isPaused: Boolean,
        stateCode: String,
        detailCode: String? = null
    ) {
        executor.execute {
            val dao = AppDatabase.getDatabase(context).automationStateDao()
            val current = dao.getState() ?: AutomationState()
            dao.saveState(
                current.copy(
                    isPaused = isPaused,
                    lastStateCode = stateCode,
                    lastStateDetailCode = detailCode
                )
            )
        }
    }
}
