package com.soundscheduler.app

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.ui.RoutineAdapter
import com.soundscheduler.app.utils.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var routines: MutableList<Routine>
    private lateinit var adapter: RoutineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyTheme()
        setContentView(R.layout.activity_main)

        // Initialize managers
        PremiumManager.initialize(this)
        NotificationUtils.createNotificationChannel(this)
        LocationRoutineManager.initialize(this)

        routines = mutableListOf()
        adapter = RoutineAdapter(this, routines) { routine ->
            deleteRoutine(routine)
        }

        val listView = findViewById<ListView>(R.id.routineListView)
        listView.adapter = adapter

        // Upgrade button
        findViewById<Button>(R.id.upgradeButton).setOnClickListener {
            val billingManager = BillingManager(this)
            billingManager.launchPurchase(this, "premium_unlock")
        }

        // Create routine button
        findViewById<Button>(R.id.createRoutineButton).setOnClickListener {
            if (PremiumManager.isPremium() || routines.size < 3) {
                val newRoutine = Routine(
                    title = "Routine ${Date().time}",
                    type = "time"
                )
                routines.add(newRoutine)
                adapter.notifyDataSetChanged()
                if (!PremiumManager.isPremium()) {
                    AdManager.showAd(this) // ad shown on creation for free users
                }
            } else {
                Toast.makeText(this, "Free limit reached. Upgrade for more routines.", Toast.LENGTH_SHORT).show()
            }
        }

        // Restore routines on load
        val restored = BackupManager.restoreRoutines(this)
        if (restored.isNotEmpty()) {
            routines.clear()
            routines.addAll(restored)
            adapter.notifyDataSetChanged()
        }
    }

    private fun applyTheme() {
        when (ThemeManager.getTheme(this)) {
            "dark" -> setTheme(R.style.AppTheme_Dark)
            "amoled" -> setTheme(R.style.AppTheme_Dark)
            else -> setTheme(R.style.AppTheme)
        }
    }

    private fun deleteRoutine(routine: Routine) {
        routines.remove(routine)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Routine deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        BackupManager.backupRoutines(this, routines)
    }
}