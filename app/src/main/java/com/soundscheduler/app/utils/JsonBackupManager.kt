// File: JsonBackupManager.kt

package com.soundscheduler.app.utils

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.soundscheduler.app.data.Routine
import java.io.File

class JsonBackupManager(private val context: Context) {

    private val gson = Gson()

    fun backupRoutines(routines: List<Routine>, fileName: String = "routines_backup.json") {
        val json = gson.toJson(routines)
        val file = File(context.filesDir, fileName)
        file.writeText(json)
        Toast.makeText(context, "Backup completed successfully", Toast.LENGTH_SHORT).show()
    }

    fun restoreRoutines(fileName: String = "routines_backup.json"): List<Routine>? {
        return try {
            val file = File(context.filesDir, fileName)
            val json = file.readText()
            val array = gson.fromJson(json, Array<Routine>::class.java)
            Toast.makeText(context, "Restore completed successfully", Toast.LENGTH_SHORT).show()
            array.toList()
        } catch (e: Exception) {
            Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
            null
        }
    }
}