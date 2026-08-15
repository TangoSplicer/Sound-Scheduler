package com.soundscheduler.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isCompleted = 0 ORDER BY isEnabled DESC, time ASC, id DESC")
    fun getAllRoutines(): LiveData<List<Routine>>

    @Query("SELECT * FROM routines WHERE type = :type AND isCompleted = 0 AND isEnabled = 1")
    fun getActiveRoutinesByType(type: String): List<Routine>

    @Query("SELECT * FROM routines WHERE id = :routineId LIMIT 1")
    fun getRoutineById(routineId: Int): Routine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(routine: Routine): Long

    @Delete
    fun delete(routine: Routine)

    @Update
    fun update(routine: Routine)

    @Query("UPDATE routines SET isCompleted = 1 WHERE id = :routineId")
    fun markCompleted(routineId: Int)

    @Query("UPDATE routines SET isEnabled = :enabled, wasEnabledBeforeGlobalPause = 0 WHERE id = :routineId")
    fun setEnabled(routineId: Int, enabled: Boolean)

    @Query(
        "UPDATE routines SET wasEnabledBeforeGlobalPause = isEnabled, isEnabled = 0 " +
            "WHERE isCompleted = 0 AND isEnabled = 1"
    )
    fun pauseAllEnabledRoutines()

    @Query(
        "UPDATE routines SET isEnabled = 1, wasEnabledBeforeGlobalPause = 0 " +
            "WHERE isCompleted = 0 AND wasEnabledBeforeGlobalPause = 1"
    )
    fun resumeAllPreviouslyEnabledRoutines()

    @Query("DELETE FROM routines")
    fun deleteAll()
}
