package com.soundscheduler.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.ui.RoutineAdapter
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.RoutineAlarmScheduler
import com.soundscheduler.app.utils.RoutineRescheduler
import com.soundscheduler.app.viewmodel.RoutineViewModel
import java.util.Calendar

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private val viewModel: RoutineViewModel by viewModels()
    private lateinit var routineAdapter: RoutineAdapter
    private lateinit var routineListView: ListView
    private lateinit var emptyStateTextView: TextView
    private lateinit var scheduleStatusTextView: TextView
    private var activeRoutineCount = 0

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Notifications are disabled. You can still save routines, but Android will not show alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationUtils.createNotificationChannel(this)
        routineListView = findViewById(R.id.routineListView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        scheduleStatusTextView = findViewById(R.id.scheduleStatusTextView)
        routineAdapter = RoutineAdapter(this, ::confirmRoutineDeletion)
        routineListView.adapter = routineAdapter

        findViewById<MaterialButton>(R.id.createRoutineButton).setOnClickListener {
            showCreateRoutineDialog()
        }

        viewModel.allRoutines.observe(this, Observer { routines ->
            routineAdapter.submitList(routines)
            activeRoutineCount = routines.size
            routineListView.visibility = if (routines.isEmpty()) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (routines.isEmpty()) View.VISIBLE else View.GONE
            updateScheduleStatus()
        })
    }

    override fun onResume() {
        super.onResume()
        RoutineRescheduler.rescheduleActiveTimeRoutines(this)
        updateScheduleStatus()
    }

    private fun showCreateRoutineDialog() {
        val content = LayoutInflater.from(this).inflate(R.layout.dialog_create_routine, null)
        val nameInput = content.findViewById<TextInputEditText>(R.id.routineNameEditText)
        val timePicker = content.findViewById<TimePicker>(R.id.routineTimePicker)
        val recurrenceSpinner = content.findViewById<Spinner>(R.id.recurrenceSpinner)
        timePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(this))

        val defaults = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timePicker.hour = defaults.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = defaults.get(Calendar.MINUTE)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.create_routine_title)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_routine, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = nameInput.text?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    nameInput.error = getString(R.string.routine_name_required)
                    return@setOnClickListener
                }

                requestNotificationPermissionIfNeeded()
                val recurrence = recurrenceForPosition(recurrenceSpinner.selectedItemPosition)
                val scheduledAt = scheduleTimeFor(
                    hour = timePicker.hour,
                    minute = timePicker.minute,
                    recurrence = recurrence
                )
                val routine = Routine(
                    title = title,
                    type = Routine.TYPE_TIME,
                    time = scheduledAt,
                    recurrence = recurrence
                )

                viewModel.insert(routine) { persistedRoutine ->
                    runOnUiThread {
                        val result = RoutineAlarmScheduler.schedule(this, persistedRoutine)
                        if (result?.exact == true) {
                            Toast.makeText(this, R.string.routine_created_exact, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, R.string.routine_created_inexact, Toast.LENGTH_LONG).show()
                            showExactAlarmGuidanceIfNeeded()
                        }
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmRoutineDeletion(routine: Routine) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_routine_title)
            .setMessage(R.string.delete_routine_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                RoutineAlarmScheduler.cancel(this, routine)
                viewModel.delete(routine) {
                    runOnUiThread {
                        Toast.makeText(this, R.string.routine_deleted, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showExactAlarmGuidanceIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            RoutineAlarmScheduler.canScheduleExactAlarms(this)
        ) return

        AlertDialog.Builder(this)
            .setTitle(R.string.exact_alarm_title)
            .setMessage(R.string.exact_alarm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .show()
    }

    private fun updateScheduleStatus() {
        scheduleStatusTextView.text = when {
            activeRoutineCount == 0 -> getString(R.string.status_no_routines)
            RoutineAlarmScheduler.canScheduleExactAlarms(this) ->
                getString(R.string.status_exact_enabled, activeRoutineCount)
            else -> getString(R.string.status_exact_disabled, activeRoutineCount)
        }
    }

    private fun recurrenceForPosition(position: Int): String? = when (position) {
        1 -> Routine.RECURRENCE_DAILY
        2 -> Routine.RECURRENCE_WEEKLY
        3 -> Routine.RECURRENCE_MONTHLY
        else -> null
    }

    private fun scheduleTimeFor(hour: Int, minute: Int, recurrence: String?): Long {
        val now = Calendar.getInstance()
        val scheduled = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (scheduled.after(now)) return scheduled.timeInMillis

        when (recurrence) {
            Routine.RECURRENCE_DAILY -> scheduled.add(Calendar.DAY_OF_YEAR, 1)
            Routine.RECURRENCE_WEEKLY -> scheduled.add(Calendar.WEEK_OF_YEAR, 1)
            Routine.RECURRENCE_MONTHLY -> scheduled.add(Calendar.MONTH, 1)
            else -> scheduled.add(Calendar.DAY_OF_YEAR, 1)
        }
        return scheduled.timeInMillis
    }
}
