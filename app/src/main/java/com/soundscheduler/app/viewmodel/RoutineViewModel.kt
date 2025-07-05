package com.soundscheduler.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.data.RoutineRepository
import com.soundscheduler.app.utils.PremiumManager

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoutineRepository(application)
    val allRoutines: LiveData<List<Routine>> = repository.getAllRoutines()

    fun insert(routine: Routine): Boolean {
        val canCreate = canCreateRoutine()
        if (canCreate) {
            repository.insert(routine)
        }
        return canCreate
    }

    fun delete(routine: Routine) {
        repository.delete(routine)
    }

    private fun canCreateRoutine(): Boolean {
        val currentCount = allRoutines.value?.size ?: 0
        val limit = PremiumManager.getRoutineLimit()
        return currentCount < limit
    }
}