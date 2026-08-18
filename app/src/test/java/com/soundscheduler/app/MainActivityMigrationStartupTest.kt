package com.soundscheduler.app

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.soundscheduler.app.data.AppDatabase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityMigrationStartupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun cleanUp() {
        context.deleteDatabase(DATABASE_NAME)
        resetDatabaseSingleton()
    }

    @Test
    fun `home screen opens after upgrading a version 8 database with enabled daily routines`() {
        context.deleteDatabase(DATABASE_NAME)
        resetDatabaseSingleton()
        createVersion8DatabaseWithDailyRoutines()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                requireNotNull(activity.findViewById<android.view.View>(com.soundscheduler.app.R.id.routineListView))
                requireNotNull(activity.findViewById<android.view.View>(com.soundscheduler.app.R.id.automationCard))
            }
        }
    }

    private fun createVersion8DatabaseWithDailyRoutines() {
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                "CREATE TABLE routines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, " +
                    "time INTEGER, location TEXT, latitude REAL, longitude REAL, radiusMeters INTEGER, " +
                    "locationTransition TEXT, chargingTransition TEXT, calendarEventId TEXT, isCompleted INTEGER NOT NULL, " +
                    "recurrence TEXT, soundProfile TEXT NOT NULL, isEnabled INTEGER NOT NULL, " +
                    "lastAttemptAtMillis INTEGER, lastOutcomeAtMillis INTEGER, lastOutcomeCode TEXT, " +
                    "lastObservedMode TEXT, lastOutcomeDetailCode TEXT, lastExecutionId INTEGER, " +
                    "wasEnabledBeforeGlobalPause INTEGER NOT NULL, daysOfWeek TEXT)"
            )
            db.execSQL(
                "CREATE TABLE routine_executions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routineId INTEGER NOT NULL, triggerType TEXT NOT NULL, " +
                    "requestedMode TEXT NOT NULL, occurredAtMillis INTEGER NOT NULL, scheduledForAtMillis INTEGER, " +
                    "outcomeCode TEXT NOT NULL, observedMode TEXT, detailCode TEXT, " +
                    "FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX index_routine_executions_routineId_occurredAtMillis ON routine_executions(routineId, occurredAtMillis)")
            db.execSQL("CREATE INDEX index_routine_executions_occurredAtMillis ON routine_executions(occurredAtMillis)")
            db.execSQL("CREATE INDEX index_routine_executions_outcomeCode_occurredAtMillis ON routine_executions(outcomeCode, occurredAtMillis)")
            db.execSQL(
                "CREATE TABLE automation_state (id INTEGER NOT NULL, isPaused INTEGER NOT NULL, lastArmedAtMillis INTEGER, " +
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

    private fun resetDatabaseSingleton() {
        val instanceField = AppDatabase::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private companion object {
        const val DATABASE_NAME = "sound_scheduler_db"
    }
}
