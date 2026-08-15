package com.soundscheduler.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AutomationStateDao {
    @Query("SELECT * FROM automation_state WHERE id = :stateId LIMIT 1")
    fun observeState(stateId: Int = AutomationState.SINGLETON_ID): LiveData<AutomationState?>

    @Query("SELECT * FROM automation_state WHERE id = :stateId LIMIT 1")
    fun getState(stateId: Int = AutomationState.SINGLETON_ID): AutomationState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveState(state: AutomationState)

    @Query(
        "UPDATE automation_state SET " +
            "isPaused = :isPaused, " +
            "lastArmedAtMillis = :lastArmedAtMillis, " +
            "lastActiveAtMillis = :lastActiveAtMillis, " +
            "lastStateCode = :stateCode, " +
            "lastStateDetailCode = :detailCode " +
            "WHERE id = :stateId"
    )
    fun updateState(
        stateId: Int = AutomationState.SINGLETON_ID,
        isPaused: Boolean,
        lastArmedAtMillis: Long?,
        lastActiveAtMillis: Long?,
        stateCode: String,
        detailCode: String?
    )
}
