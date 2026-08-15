package com.soundscheduler.app.data

import android.app.Application
import androidx.lifecycle.LiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RoutineRepository(application: Application) {
    private val routineDao: RoutineDao = AppDatabase.getDatabase(application).routineDao()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun getAllRoutines(): LiveData<List<Routine>> = routineDao.getAllRoutines()

    fun insert(routine: Routine, onInserted: (Routine) -> Unit = {}) {
        executor.execute {
            val id = routineDao.insert(routine).toInt()
            onInserted(routine.copy(id = id))
        }
    }

    fun delete(routine: Routine, onDeleted: () -> Unit = {}) {
        executor.execute {
            routineDao.delete(routine)
            onDeleted()
        }
    }

    fun update(routine: Routine, onUpdated: () -> Unit = {}) {
        executor.execute {
            routineDao.update(routine)
            onUpdated()
        }
    }

    fun markCompleted(routineId: Int) {
        executor.execute { routineDao.markCompleted(routineId) }
    }

    fun setEnabled(routineId: Int, enabled: Boolean, onUpdated: () -> Unit = {}) {
        executor.execute {
            routineDao.setEnabled(routineId, enabled)
            onUpdated()
        }
    }

    fun deleteAll() {
        executor.execute { routineDao.deleteAll() }
    }
}
