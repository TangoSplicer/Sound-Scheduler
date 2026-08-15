package com.soundscheduler.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RoutineExecutionDao {
    @Query(
        "SELECT routine_executions.*, routines.title AS routineTitle " +
            "FROM routine_executions INNER JOIN routines ON routines.id = routine_executions.routineId " +
            "ORDER BY occurredAtMillis DESC, routine_executions.id DESC"
    )
    fun getAllExecutionDetails(): LiveData<List<RoutineExecutionDetail>>

    @Query(
        "SELECT routine_executions.*, routines.title AS routineTitle " +
            "FROM routine_executions INNER JOIN routines ON routines.id = routine_executions.routineId " +
            "WHERE routineId = :routineId ORDER BY occurredAtMillis DESC, routine_executions.id DESC"
    )
    fun getExecutionDetailsForRoutine(routineId: Int): LiveData<List<RoutineExecutionDetail>>

    @Query(
        "SELECT routine_executions.*, routines.title AS routineTitle " +
            "FROM routine_executions INNER JOIN routines ON routines.id = routine_executions.routineId " +
            "WHERE outcomeCode != :appliedOutcome ORDER BY occurredAtMillis DESC, routine_executions.id DESC"
    )
    fun getAttentionExecutionDetails(
        appliedOutcome: String = RoutineExecution.OUTCOME_APPLIED
    ): LiveData<List<RoutineExecutionDetail>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertExecution(execution: RoutineExecution): Long

    @Query(
        "UPDATE routines SET " +
            "lastAttemptAtMillis = :attemptAtMillis, " +
            "lastOutcomeAtMillis = :outcomeAtMillis, " +
            "lastOutcomeCode = :outcomeCode, " +
            "lastObservedMode = :observedMode, " +
            "lastOutcomeDetailCode = :detailCode, " +
            "lastExecutionId = :executionId " +
            "WHERE id = :routineId"
    )
    fun updateRoutineSummary(
        routineId: Int,
        attemptAtMillis: Long,
        outcomeAtMillis: Long,
        outcomeCode: String,
        observedMode: String?,
        detailCode: String?,
        executionId: Long
    )

    @Query("DELETE FROM routine_executions WHERE occurredAtMillis < :cutoffMillis")
    fun deleteExecutionsOlderThan(cutoffMillis: Long)

    @Query(
        "DELETE FROM routine_executions WHERE id NOT IN (" +
            "SELECT id FROM routine_executions ORDER BY occurredAtMillis DESC, id DESC LIMIT :keepCount" +
            ")"
    )
    fun trimToMostRecent(keepCount: Int)

    @Query("DELETE FROM routine_executions")
    fun clearAllExecutions()

    @Transaction
    fun recordExecutionAndUpdateSummary(
        execution: RoutineExecution,
        retentionCutoffMillis: Long,
        retentionCount: Int
    ): Long {
        val executionId = insertExecution(execution)
        updateRoutineSummary(
            routineId = execution.routineId,
            attemptAtMillis = execution.occurredAtMillis,
            outcomeAtMillis = execution.occurredAtMillis,
            outcomeCode = execution.outcomeCode,
            observedMode = execution.observedMode,
            detailCode = execution.detailCode,
            executionId = executionId
        )
        deleteExecutionsOlderThan(retentionCutoffMillis)
        trimToMostRecent(retentionCount)
        return executionId
    }
}
