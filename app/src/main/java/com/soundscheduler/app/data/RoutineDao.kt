package com.soundscheduler.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isCompleted = 0 ORDER BY isEnabled DESC, time ASC, id DESC")
    fun getAllRoutines(): LiveData<List<Routine>>

    @Query("SELECT * FROM routines WHERE type = :type AND isCompleted = 0 AND isEnabled = 1")
    fun getActiveRoutinesByType(type: String): List<Routine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(routine: Routine): Long

    @Delete
    fun delete(routine: Routine)

    @Query("UPDATE routines SET isCompleted = 1 WHERE id = :routineId")
    fun markCompleted(routineId: Int)

    @Query("UPDATE routines SET isEnabled = :enabled WHERE id = :routineId")
    fun setEnabled(routineId: Int, enabled: Boolean)

    @Query("DELETE FROM routines")
    fun deleteAll()
}
