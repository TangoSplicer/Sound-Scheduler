package com.soundscheduler.app.utils

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.soundscheduler.app.data.Routine
import java.io.File

object ArchiveManager {

    private const val ARCHIVE_FILE_NAME = "sound_scheduler_archive.json"

    fun archiveRoutine(context: Context, routine: Routine) {
        if (!PremiumManager.isPremium()) {
            Toast.makeText(context, "Upgrade to premium to archive routines.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentArchive = getArchivedRoutines(context).toMutableList()
        currentArchive.add(routine)

        try {
            val json = Gson().toJson(currentArchive)
            val file = File(context.filesDir, ARCHIVE_FILE_NAME)
            file.writeText(json)
            Toast.makeText(context, "Routine archived.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Archive failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Failed to load archives: ${e.message}", Toast.LENGTH_SHORT).show()
            emptyList()
        }
    }
}