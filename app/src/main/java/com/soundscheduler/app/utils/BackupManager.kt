package com.soundscheduler.app.utils

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.soundscheduler.app.data.Routine
import java.io.File

object BackupManager {

    private const val BACKUP_FILE_NAME = "sound_scheduler_backup.json"

    fun backupRoutines(context: Context, routines: List<Routine>) {
        try {
            val json = Gson().toJson(routines)
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            file.writeText(json)
            Toast.makeText(context, "Backup created", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreRoutines(context: Context): List<Routine> {
        return try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            if (file.exists()) {
                val json = file.readText()
                Gson().fromJson(json, Array<Routine>::class.java).toList()
            } else {
                Toast.makeText(context, "No backup found", Toast.LENGTH_SHORT).show()
                emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            emptyList()
        }
    }
}