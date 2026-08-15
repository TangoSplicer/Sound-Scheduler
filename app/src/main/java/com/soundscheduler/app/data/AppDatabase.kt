package com.soundscheduler.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Routine::class, RoutineExecution::class, AutomationState::class],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun routineExecutionDao(): RoutineExecutionDao
    abstract fun automationStateDao(): AutomationStateDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE routines ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE routines ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE routines ADD COLUMN radiusMeters INTEGER")
                db.execSQL("ALTER TABLE routines ADD COLUMN locationTransition TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN lastAttemptAtMillis INTEGER")
                db.execSQL("ALTER TABLE routines ADD COLUMN lastOutcomeAtMillis INTEGER")
                db.execSQL("ALTER TABLE routines ADD COLUMN lastOutcomeCode TEXT")
                db.execSQL("ALTER TABLE routines ADD COLUMN lastObservedMode TEXT")
                db.execSQL("ALTER TABLE routines ADD COLUMN lastOutcomeDetailCode TEXT")
                db.execSQL("ALTER TABLE routines ADD COLUMN lastExecutionId INTEGER")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS automation_state (" +
                        "id INTEGER NOT NULL, " +
                        "isPaused INTEGER NOT NULL DEFAULT 0, " +
                        "lastArmedAtMillis INTEGER, " +
                        "lastActiveAtMillis INTEGER, " +
                        "lastStateCode TEXT NOT NULL DEFAULT 'off', " +
                        "lastStateDetailCode TEXT, " +
                        "PRIMARY KEY(id))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO automation_state " +
                        "(id, isPaused, lastStateCode) VALUES (1, 0, 'off')"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS routine_executions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "routineId INTEGER NOT NULL, " +
                        "triggerType TEXT NOT NULL, " +
                        "requestedMode TEXT NOT NULL, " +
                        "occurredAtMillis INTEGER NOT NULL, " +
                        "scheduledForAtMillis INTEGER, " +
                        "outcomeCode TEXT NOT NULL, " +
                        "observedMode TEXT, " +
                        "detailCode TEXT, " +
                        "FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_routine_executions_routineId_occurredAtMillis " +
                        "ON routine_executions(routineId, occurredAtMillis)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_routine_executions_occurredAtMillis " +
                        "ON routine_executions(occurredAtMillis)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_routine_executions_outcomeCode_occurredAtMillis " +
                        "ON routine_executions(outcomeCode, occurredAtMillis)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE routines ADD COLUMN wasEnabledBeforeGlobalPause " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN chargingTransition TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sound_scheduler_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
