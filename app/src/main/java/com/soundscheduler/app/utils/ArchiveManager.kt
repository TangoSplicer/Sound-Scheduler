// File: app/src/main/java/com/soundscheduler/app/utils/ArchiveManager.kt
package com.soundscheduler.app.utils

// ← import PremiumManager so PremiumManager.isPremium() resolves
import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.soundscheduler.app.data.Routine
import java.io.File

object ArchiveManager {

    private const val ARCHIVE_FILE_NAME = "sound_scheduler_archive.json"

    fun archiveRoutine(context: Context, routine: Routine) {
        if (!PremiumManager.isPremium()) {
            Toast.makeText(
                context,
                "Upgrade to premium to archive routines.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val file = File(context.filesDir, ARCHIVE_FILE_NAME)
            val current = if (file.exists()) {
                Gson().fromJson(file.readText(), Array<Routine>::class.java).toMutableList()
            } else {
                mutableListOf()
            }
            current.add(routine)
            file.writeText(Gson().toJson(current))
            Toast.makeText(context, "Routine archived.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to archive routine: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun getArchivedRoutines(context: Context): List<Routine> {
        return try {
            val file = File(context.filesDir, ARCHIVE_FILE_NAME)
            if (file.exists()) {
                val json = file.readText()
                Gson().fromJson(json, Array<Routine>::class.java).toList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to load archives: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            emptyList()
        }
    }
}
