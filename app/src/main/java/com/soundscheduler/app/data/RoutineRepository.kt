package com.soundscheduler.app.data

import android.app.Application
import androidx.lifecycle.LiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RoutineRepository(application: Application) {

    private val routineDao: RoutineDao
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        val db = AppDatabase.getDatabase(application)
        routineDao = db.routineDao()
    }

    fun getAllRoutines(): LiveData<List<Routine>> = routineDao.getAllRoutines()

    fun insert(routine: Routine) {
        executor.execute { routineDao.insert(routine) }
    }

    fun delete(routine: Routine) {
        executor.execute { routineDao.delete(routine) }
    }

    fun deleteAll() {
        executor.execute { routineDao.deleteAll() }
    }
}