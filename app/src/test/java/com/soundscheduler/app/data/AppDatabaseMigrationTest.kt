package com.soundscheduler.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun cleanUp() {
        context.deleteDatabase(DATABASE_NAME)
        resetSingleton()
    }

    @Test
    fun `opening an original version 1 database migrates through every version and preserves a daily routine`() {
        context.deleteDatabase(DATABASE_NAME)
        resetSingleton()
        createVersion1DatabaseWithDailyRoutine()

        val database = AppDatabase.getDatabase(context)
        database.openHelper.writableDatabase

        val routine = readRoutine(database, 1)
        assertNotNull(routine)
        assertEquals(Routine.TYPE_TIME, routine?.type)
        assertEquals(Routine.RECURRENCE_DAILY, routine?.recurrence)
        assertEquals(Routine.PROFILE_VIBRATE, routine?.soundProfile)
        assertEquals(true, routine?.isEnabled)
        database.close()
    }

    @Test
    fun `opening a version 8 database migrates it and preserves two enabled daily routines`() {
        context.deleteDatabase(DATABASE_NAME)
        resetSingleton()
        createVersion8DatabaseWithDailyRoutines()

        val database = AppDatabase.getDatabase(context)
        database.openHelper.writableDatabase

        val morning = readRoutine(database, 1)
        val evening = readRoutine(database, 2)

        assertNotNull(morning)
        assertNotNull(evening)
        assertEquals(Routine.TYPE_TIME, morning?.type)
        assertEquals(Routine.RECURRENCE_DAILY, morning?.recurrence)
        assertEquals(Routine.PROFILE_RING, morning?.soundProfile)
        assertEquals(true, morning?.isEnabled)
        assertEquals(Routine.TYPE_TIME, evening?.type)
        assertEquals(Routine.RECURRENCE_DAILY, evening?.recurrence)
        assertEquals(Routine.PROFILE_VIBRATE, evening?.soundProfile)
        assertEquals(true, evening?.isEnabled)

        database.close()
    }

    private fun createVersion1DatabaseWithDailyRoutine() {
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                "CREATE TABLE routines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, " +
                    "time INTEGER, location TEXT, calendarEventId TEXT, isCompleted INTEGER NOT NULL, " +
                    "recurrence TEXT, soundProfile TEXT NOT NULL)"
            )
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES(42, 'dd8e8cf96aaa94ae53924aaf90de259f')")
            db.execSQL("PRAGMA user_version = 1")
            db.execSQL(
                "INSERT INTO routines (id, title, type, time, isCompleted, recurrence, soundProfile) " +
                    "VALUES (1, 'Original daily vibrate', 'time', 1760000000000, 0, 'daily', 'vibrate')"
            )
        }
    }

    private fun createVersion8DatabaseWithDailyRoutines() {
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                "CREATE TABLE routines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT NOT NULL, type TEXT NOT NULL, time INTEGER, location TEXT, " +
                    "latitude REAL, longitude REAL, radiusMeters INTEGER, locationTransition TEXT, " +
                    "chargingTransition TEXT, calendarEventId TEXT, isCompleted INTEGER NOT NULL, " +
                    "recurrence TEXT, soundProfile TEXT NOT NULL, isEnabled INTEGER NOT NULL, " +
                    "lastAttemptAtMillis INTEGER, lastOutcomeAtMillis INTEGER, lastOutcomeCode TEXT, " +
                    "lastObservedMode TEXT, lastOutcomeDetailCode TEXT, lastExecutionId INTEGER, " +
                    "wasEnabledBeforeGlobalPause INTEGER NOT NULL, daysOfWeek TEXT)"
            )
            db.execSQL(
                "CREATE TABLE routine_executions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routineId INTEGER NOT NULL, " +
                    "triggerType TEXT NOT NULL, requestedMode TEXT NOT NULL, occurredAtMillis INTEGER NOT NULL, " +
                    "scheduledForAtMillis INTEGER, outcomeCode TEXT NOT NULL, observedMode TEXT, detailCode TEXT, " +
                    "FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX index_routine_executions_routineId_occurredAtMillis ON routine_executions(routineId, occurredAtMillis)")
            db.execSQL("CREATE INDEX index_routine_executions_occurredAtMillis ON routine_executions(occurredAtMillis)")
            db.execSQL("CREATE INDEX index_routine_executions_outcomeCode_occurredAtMillis ON routine_executions(outcomeCode, occurredAtMillis)")
            db.execSQL(
                "CREATE TABLE automation_state (" +
                    "id INTEGER NOT NULL, isPaused INTEGER NOT NULL, lastArmedAtMillis INTEGER, " +
                    "lastActiveAtMillis INTEGER, lastStateCode TEXT NOT NULL, lastStateDetailCode TEXT, " +
                    "pauseUntilMillis INTEGER, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES(42, 'c1c8e90e768ff363c1582d1d21520e9a')")
            db.execSQL("PRAGMA user_version = 8")
            db.execSQL(
                "INSERT INTO routines (id, title, type, time, isCompleted, recurrence, soundProfile, isEnabled, wasEnabledBeforeGlobalPause) " +
                    "VALUES (1, 'Morning ring', 'time', 1760000000000, 0, 'daily', 'ring', 1, 0)"
            )
            db.execSQL(
                "INSERT INTO routines (id, title, type, time, isCompleted, recurrence, soundProfile, isEnabled, wasEnabledBeforeGlobalPause) " +
                    "VALUES (2, 'Evening vibrate', 'time', 1760040000000, 0, 'daily', 'vibrate', 1, 0)"
            )
        }
    }

    private fun readRoutine(database: AppDatabase, id: Int): Routine? {
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        return try {
            executor.submit<Routine?> { database.routineDao().getRoutineById(id) }.get()
        } finally {
            executor.shutdown()
        }
    }

    private fun resetSingleton() {
        val instanceField = AppDatabase::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private companion object {
        const val DATABASE_NAME = "sound_scheduler_db"
    }
}
