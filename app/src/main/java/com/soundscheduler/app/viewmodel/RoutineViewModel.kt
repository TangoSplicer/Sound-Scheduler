package com.soundscheduler.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.data.RoutineRepository

class RoutineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoutineRepository(application)

    val allRoutines: LiveData<List<Routine>> = repository.getAllRoutines()

    fun insert(routine: Routine, onInserted: (Routine) -> Unit = {}) {
        repository.insert(routine, onInserted)
    }

    fun delete(routine: Routine, onDeleted: () -> Unit = {}) {
        repository.delete(routine, onDeleted)
    }

    fun setEnabled(routineId: Int, enabled: Boolean, onUpdated: () -> Unit = {}) {
        repository.setEnabled(routineId, enabled, onUpdated)
    }
}
