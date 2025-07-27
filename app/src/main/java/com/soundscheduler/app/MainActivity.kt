// app/src/main/java/com/soundscheduler/app/MainActivity.kt
package com.soundscheduler.app

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.ui.RoutineAdapter
import com.soundscheduler.app.utils.BackupManager
import com.soundscheduler.app.utils.LocationRoutineManager
import com.soundscheduler.app.utils.NotificationUtils
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var routines: MutableList<Routine>
    private lateinit var adapter: RoutineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize non-billing managers
        NotificationUtils.createNotificationChannel(this)
        LocationRoutineManager.initialize(this)

        // Set up the list and adapter
        routines = mutableListOf()
        adapter = RoutineAdapter(this, routines)
        findViewById<ListView>(R.id.routineListView).adapter = adapter

        // Create routine button (no more premium checks or ads)
        findViewById<Button>(R.id.createRoutineButton).setOnClickListener {
            val newRoutine = Routine(
                title = "Routine ${Date().time}",
                type = "time"
            )
            routines.add(newRoutine)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Backup routines on exit
        BackupManager.backupRoutines(this, routines)
    }
}
