package com.soundscheduler.app.data

import android.content.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Local-only execution history with bounded retention. It deliberately stores no location trail,
 * addresses, device IDs, or raw Android error messages.
 */
object ExecutionHistoryRepository {
    private const val RETENTION_DAYS = 30L
    private const val RETENTION_COUNT = 100
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun record(
        context: Context,
        routineId: Int,
        triggerType: String,
        requestedMode: String,
        outcomeCode: String,
        observedMode: String? = null,
        detailCode: String? = null,
        scheduledForAtMillis: Long? = null
    ) {
        if (routineId <= 0) return
        executor.execute {
            recordNow(
                context = context,
                routineId = routineId,
                triggerType = triggerType,
                requestedMode = requestedMode,
                outcomeCode = outcomeCode,
                observedMode = observedMode,
                detailCode = detailCode,
                scheduledForAtMillis = scheduledForAtMillis
            )
        }
    }

    fun recordAutomationRearmRequired(
        context: Context,
        routineId: Int,
        triggerType: String
    ) {
        if (routineId <= 0) return
        executor.execute {
            val routine = AppDatabase.getDatabase(context).routineDao().getRoutineById(routineId) ?: return@execute
            if (!routine.isEnabled || routine.isCompleted) return@execute
            recordNow(
                context = context,
                routineId = routine.id,
                triggerType = triggerType,
                requestedMode = routine.targetSoundMode(),
                outcomeCode = RoutineExecution.OUTCOME_AUTOMATION_REARM_REQUIRED,
                detailCode = "foreground_service_not_ready",
                scheduledForAtMillis = if (routine.type == Routine.TYPE_TIME) routine.time else null,
                observedMode = null
            )
        }
    }

    fun recordForRoutine(
        context: Context,
        routine: Routine,
        triggerType: String,
        outcomeCode: String,
        observedMode: String? = null,
        detailCode: String? = null
    ) {
        record(
            context = context,
            routineId = routine.id,
            triggerType = triggerType,
            requestedMode = routine.targetSoundMode(),
            outcomeCode = outcomeCode,
            observedMode = observedMode,
            detailCode = detailCode,
            scheduledForAtMillis = if (routine.type == Routine.TYPE_TIME) routine.time else null
        )
    }

    /** Call only from an existing background worker before completing a one-time routine. */
    fun recordForRoutineNow(
        context: Context,
        routine: Routine,
        triggerType: String,
        outcomeCode: String,
        observedMode: String? = null,
        detailCode: String? = null
    ) {
        recordNow(
            context = context,
            routineId = routine.id,
            triggerType = triggerType,
            requestedMode = routine.targetSoundMode(),
            outcomeCode = outcomeCode,
            observedMode = observedMode,
            detailCode = detailCode,
            scheduledForAtMillis = if (routine.type == Routine.TYPE_TIME) routine.time else null
        )
    }

    fun clearAll(context: Context, onCleared: () -> Unit = {}) {
        executor.execute {
            AppDatabase.getDatabase(context).routineExecutionDao().clearAllExecutions()
            onCleared()
        }
    }

    private fun recordNow(
        context: Context,
        routineId: Int,
        triggerType: String,
        requestedMode: String,
        outcomeCode: String,
        observedMode: String?,
        detailCode: String?,
        scheduledForAtMillis: Long?
    ) {
        val now = System.currentTimeMillis()
        val execution = RoutineExecution(
            routineId = routineId,
            triggerType = triggerType,
            requestedMode = requestedMode,
            occurredAtMillis = now,
            scheduledForAtMillis = scheduledForAtMillis,
            outcomeCode = outcomeCode,
            observedMode = observedMode,
            detailCode = detailCode
        )
        val database = AppDatabase.getDatabase(context)
        database.routineExecutionDao().recordExecutionAndUpdateSummary(
            execution = execution,
            retentionCutoffMillis = now - RETENTION_DAYS * MILLIS_PER_DAY,
            retentionCount = RETENTION_COUNT
        )
    }
}
