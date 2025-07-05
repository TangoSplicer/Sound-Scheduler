package com.soundscheduler.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY id DESC")
    fun getAllRoutines(): LiveData<List<Routine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(routine: Routine)

    @Delete
    fun delete(routine: Routine)

    @Query("DELETE FROM routines")
    fun deleteAll()
}