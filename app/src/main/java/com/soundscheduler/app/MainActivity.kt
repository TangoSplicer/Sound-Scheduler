package com.soundscheduler.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.ui.RoutineAdapter
import com.soundscheduler.app.utils.LocationRoutineManager
import com.soundscheduler.app.utils.NotificationUtils
import com.soundscheduler.app.utils.RoutineAlarmScheduler
import com.soundscheduler.app.utils.RoutineRescheduler
import com.soundscheduler.app.utils.SoundModeController
import com.soundscheduler.app.viewmodel.RoutineViewModel
import java.util.Calendar

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private val viewModel: RoutineViewModel by viewModels()
    private lateinit var routineAdapter: RoutineAdapter
    private lateinit var routineListView: ListView
    private lateinit var emptyStateTextView: TextView
    private lateinit var scheduleStatusTextView: TextView
    private lateinit var soundAccessButton: MaterialButton
    private var activeRoutineCount = 0
    private var activeTimeRoutineCount = 0
    private var activeLocationRoutineCount = 0
    private var awaitingSoundAccessResult = false
    private var awaitingLocationAccessResult = false
    private var pendingLocationCaptureAction: (() -> Unit)? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Sound routines will still work. Android just will not show routine confirmations.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (LocationRoutineManager.hasForegroundLocationAccess(this)) {
            pendingLocationCaptureAction?.invoke()
        } else {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show()
        }
        pendingLocationCaptureAction = null
        updateScheduleStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        awaitingSoundAccessResult = savedInstanceState?.getBoolean(STATE_AWAITING_SOUND_ACCESS, false) ?: false
        awaitingLocationAccessResult =
            savedInstanceState?.getBoolean(STATE_AWAITING_LOCATION_ACCESS, false) ?: false
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        NotificationUtils.createNotificationChannel(this)
        routineListView = findViewById(R.id.routineListView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        scheduleStatusTextView = findViewById(R.id.scheduleStatusTextView)
        soundAccessButton = findViewById(R.id.soundAccessButton)
        routineAdapter = RoutineAdapter(this, ::confirmRoutineDeletion, ::setRoutineEnabled)
        routineListView.adapter = routineAdapter

        findViewById<MaterialButton>(R.id.createRoutineButton).setOnClickListener {
            showCreateRoutineDialog()
        }
        findViewById<MaterialButton>(R.id.quickRingButton).setOnClickListener {
            applySoundModeNow(Routine.PROFILE_RING)
        }
        findViewById<MaterialButton>(R.id.quickVibrateButton).setOnClickListener {
            applySoundModeNow(Routine.PROFILE_VIBRATE)
        }
        findViewById<MaterialButton>(R.id.quickSilentButton).setOnClickListener {
            applySoundModeNow(Routine.PROFILE_SILENT)
        }
        soundAccessButton.setOnClickListener { openSoundAccessSettings() }

        viewModel.allRoutines.observe(this, Observer { routines ->
            routineAdapter.submitList(routines)
            activeRoutineCount = routines.count { it.isEnabled }
            activeTimeRoutineCount = routines.count { it.isEnabled && it.type == Routine.TYPE_TIME }
            activeLocationRoutineCount = routines.count { it.isEnabled && it.type == Routine.TYPE_LOCATION }
            routineListView.visibility = if (routines.isEmpty()) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (routines.isEmpty()) View.VISIBLE else View.GONE
            updateScheduleStatus()
        })
    }

    override fun onResume() {
        super.onResume()
        val returnedFromSoundAccessSettings = awaitingSoundAccessResult
        val returnedFromLocationAccessSettings = awaitingLocationAccessResult
        val hasSoundAccess = SoundModeController.hasNotificationPolicyAccess(this)
        val hasLocationAccess = LocationRoutineManager.hasRequiredLocationAccess(this)

        if (hasSoundAccess || hasLocationAccess) {
            RoutineRescheduler.rescheduleActiveRoutines(this)
        }
        updateScheduleStatus()

        if (returnedFromSoundAccessSettings) {
            awaitingSoundAccessResult = false
            Toast.makeText(
                this,
                if (hasSoundAccess) R.string.sound_access_granted else R.string.sound_access_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
        if (returnedFromLocationAccessSettings) {
            awaitingLocationAccessResult = false
            Toast.makeText(
                this,
                if (hasLocationAccess) R.string.location_access_granted else R.string.location_access_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_AWAITING_SOUND_ACCESS, awaitingSoundAccessResult)
        outState.putBoolean(STATE_AWAITING_LOCATION_ACCESS, awaitingLocationAccessResult)
        super.onSaveInstanceState(outState)
    }

    private fun applySystemBarInsets() {
        val content = findViewById<View>(R.id.mainContent)
        val basePadding = resources.getDimensionPixelSize(R.dimen.screen_content_padding)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val safeArea = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                basePadding + safeArea.left,
                basePadding + safeArea.top,
                basePadding + safeArea.right,
                basePadding + safeArea.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    private fun showCreateRoutineDialog() {
        val content = LayoutInflater.from(this).inflate(R.layout.dialog_create_routine, null)
        val nameInput = content.findViewById<TextInputEditText>(R.id.routineNameEditText)
        val soundModeSpinner = content.findViewById<Spinner>(R.id.soundModeSpinner)
        val routineTypeRadioGroup = content.findViewById<RadioGroup>(R.id.routineTypeRadioGroup)
        val timeSection = content.findViewById<LinearLayout>(R.id.timeRoutineSection)
        val locationSection = content.findViewById<LinearLayout>(R.id.locationRoutineSection)
        val timePicker = content.findViewById<TimePicker>(R.id.routineTimePicker)
        val recurrenceSpinner = content.findViewById<Spinner>(R.id.recurrenceSpinner)
        val captureLocationButton = content.findViewById<MaterialButton>(R.id.captureLocationButton)
        val locationLabelInput = content.findViewById<TextInputEditText>(R.id.locationLabelEditText)
        val radiusSpinner = content.findViewById<Spinner>(R.id.locationRadiusSpinner)
        val transitionSpinner = content.findViewById<Spinner>(R.id.locationTransitionSpinner)
        var capturedLocation: Location? = null

        timePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(this))
        radiusSpinner.setSelection(1)
        val defaults = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timePicker.hour = defaults.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = defaults.get(Calendar.MINUTE)

        routineTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val useLocation = checkedId == R.id.locationRoutineRadioButton
            timeSection.visibility = if (useLocation) View.GONE else View.VISIBLE
            locationSection.visibility = if (useLocation) View.VISIBLE else View.GONE
        }
        captureLocationButton.setOnClickListener {
            val capture = {
                captureLocationButton.isEnabled = false
                captureLocationButton.setText(R.string.capturing_location)
                LocationRoutineManager.captureCurrentLocation(this) { location ->
                    runOnUiThread {
                        captureLocationButton.isEnabled = true
                        captureLocationButton.setText(R.string.capture_location)
                        capturedLocation = location
                        Toast.makeText(
                            this,
                            if (location == null) R.string.location_capture_failed else R.string.location_captured,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            if (LocationRoutineManager.hasForegroundLocationAccess(this)) {
                capture()
            } else {
                pendingLocationCaptureAction = capture
                foregroundLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

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

                val targetMode = soundModeForPosition(soundModeSpinner.selectedItemPosition)
                if (routineTypeRadioGroup.checkedRadioButtonId == R.id.locationRoutineRadioButton) {
                    val label = locationLabelInput.text?.toString()?.trim().orEmpty()
                    val location = capturedLocation
                    if (location == null || label.isBlank()) {
                        if (label.isBlank()) locationLabelInput.error = getString(R.string.location_required)
                        Toast.makeText(this, R.string.location_required, Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    val routine = Routine(
                        title = title,
                        type = Routine.TYPE_LOCATION,
                        location = label,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radiusMeters = radiusForPosition(radiusSpinner.selectedItemPosition),
                        locationTransition = transitionForPosition(transitionSpinner.selectedItemPosition),
                        recurrence = Routine.RECURRENCE_DAILY,
                        soundProfile = targetMode
                    )
                    viewModel.insert(routine) { _ ->
                        runOnUiThread {
                            LocationRoutineManager.refreshActiveGeofences(this) { registered ->
                                if (!registered && !LocationRoutineManager.hasRequiredLocationAccess(this)) {
                                    runOnUiThread { showLocationAccessGuidance() }
                                }
                            }
                            Toast.makeText(this, R.string.routine_created_location, Toast.LENGTH_LONG).show()
                            if (!SoundModeController.hasNotificationPolicyAccess(this)) {
                                showSoundAccessGuidance()
                            } else {
                                requestNotificationPermissionIfNeeded()
                            }
                            dialog.dismiss()
                        }
                    }
                    return@setOnClickListener
                }

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
                    recurrence = recurrence,
                    soundProfile = targetMode
                )

                viewModel.insert(routine) { persistedRoutine ->
                    runOnUiThread {
                        val result = RoutineAlarmScheduler.schedule(this, persistedRoutine)
                        if (result?.exact == true) {
                            Toast.makeText(this, R.string.routine_created_exact, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, R.string.routine_created_inexact, Toast.LENGTH_LONG).show()
                        }
                        if (!SoundModeController.hasNotificationPolicyAccess(this)) {
                            showSoundAccessGuidance()
                        } else {
                            requestNotificationPermissionIfNeeded()
                            if (result?.exact != true) showExactAlarmGuidanceIfNeeded()
                        }
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun setRoutineEnabled(routine: Routine, enabled: Boolean) {
        if (!enabled) {
            if (routine.type == Routine.TYPE_LOCATION) {
                LocationRoutineManager.removeGeofence(this, routine)
            } else {
                RoutineAlarmScheduler.cancel(this, routine)
            }
            viewModel.setEnabled(routine.id, false)
            Toast.makeText(this, R.string.routine_paused, Toast.LENGTH_SHORT).show()
            return
        }

        val resumedRoutine = routine.copy(isEnabled = true)
        viewModel.setEnabled(routine.id, true) {
            runOnUiThread {
                if (resumedRoutine.type == Routine.TYPE_LOCATION) {
                    LocationRoutineManager.refreshActiveGeofences(this) { registered ->
                        if (!registered && !LocationRoutineManager.hasRequiredLocationAccess(this)) {
                            runOnUiThread { showLocationAccessGuidance() }
                        }
                    }
                } else {
                    val result = RoutineAlarmScheduler.schedule(this, resumedRoutine)
                    if (SoundModeController.hasNotificationPolicyAccess(this) && result?.exact != true) {
                        showExactAlarmGuidanceIfNeeded()
                    }
                }
                if (!SoundModeController.hasNotificationPolicyAccess(this)) {
                    showSoundAccessGuidance()
                }
                Toast.makeText(this, R.string.routine_enabled, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applySoundModeNow(mode: String) {
        when (SoundModeController.applyMode(this, mode)) {
            SoundModeController.ApplyResult.APPLIED -> {
                Toast.makeText(
                    this,
                    getString(R.string.apply_mode_success, soundModeLabel(mode)),
                    Toast.LENGTH_SHORT
                ).show()
                updateScheduleStatus()
            }

            SoundModeController.ApplyResult.POLICY_ACCESS_REQUIRED -> showSoundAccessGuidance()
            SoundModeController.ApplyResult.REJECTED_BY_SYSTEM -> {
                Toast.makeText(this, R.string.apply_mode_rejected, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmRoutineDeletion(routine: Routine) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_routine_title)
            .setMessage(R.string.delete_routine_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (routine.type == Routine.TYPE_LOCATION) {
                    LocationRoutineManager.removeGeofence(this, routine)
                } else {
                    RoutineAlarmScheduler.cancel(this, routine)
                }
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

    private fun showSoundAccessGuidance() {
        AlertDialog.Builder(this)
            .setTitle(R.string.sound_access_title)
            .setMessage(R.string.sound_access_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.grant_sound_access) { _, _ -> openSoundAccessSettings() }
            .show()
    }

    private fun showLocationAccessGuidance() {
        if (LocationRoutineManager.hasRequiredLocationAccess(this)) return
        AlertDialog.Builder(this)
            .setTitle(R.string.grant_location_access)
            .setMessage(R.string.background_location_rationale)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.open_settings) { _, _ -> openAppLocationSettings() }
            .show()
    }

    private fun openSoundAccessSettings() {
        awaitingSoundAccessResult = true
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            awaitingSoundAccessResult = false
            Toast.makeText(this, R.string.sound_access_settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppLocationSettings() {
        awaitingLocationAccessResult = true
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: ActivityNotFoundException) {
            awaitingLocationAccessResult = false
            Toast.makeText(this, R.string.location_settings_unavailable, Toast.LENGTH_LONG).show()
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
        val hasSoundAccess = SoundModeController.hasNotificationPolicyAccess(this)
        val hasLocationAccess = LocationRoutineManager.hasRequiredLocationAccess(this)
        soundAccessButton.visibility = if (hasSoundAccess) View.GONE else View.VISIBLE
        val routineStatus = when {
            !hasSoundAccess -> getString(R.string.status_sound_access_required, activeRoutineCount)
            activeRoutineCount == 0 -> getString(R.string.status_no_routines)
            activeLocationRoutineCount > 0 && !hasLocationAccess ->
                getString(R.string.status_location_access_required, activeLocationRoutineCount)
            activeTimeRoutineCount == 0 ->
                getString(R.string.status_location_ready, activeLocationRoutineCount)
            RoutineAlarmScheduler.canScheduleExactAlarms(this) ->
                getString(R.string.status_exact_enabled, activeRoutineCount)
            else -> getString(R.string.status_exact_disabled, activeRoutineCount)
        }
        val currentMode = getString(
            R.string.current_sound_mode,
            soundModeLabel(SoundModeController.currentMode(this))
        )
        scheduleStatusTextView.text = getString(R.string.sound_status_format, currentMode, routineStatus)
        scheduleStatusTextView.contentDescription = scheduleStatusTextView.text
    }

    private fun recurrenceForPosition(position: Int): String? = when (position) {
        1 -> Routine.RECURRENCE_DAILY
        2 -> Routine.RECURRENCE_WEEKLY
        3 -> Routine.RECURRENCE_MONTHLY
        else -> null
    }

    private fun soundModeForPosition(position: Int): String = when (position) {
        1 -> Routine.PROFILE_VIBRATE
        2 -> Routine.PROFILE_SILENT
        else -> Routine.PROFILE_RING
    }

    private fun radiusForPosition(position: Int): Int = when (position) {
        0 -> 100
        2 -> 250
        3 -> 500
        else -> Routine.DEFAULT_LOCATION_RADIUS_METERS
    }

    private fun transitionForPosition(position: Int): String = when (position) {
        1 -> Routine.LOCATION_TRANSITION_EXIT
        else -> Routine.LOCATION_TRANSITION_ENTER
    }

    private fun soundModeLabel(mode: String): String = when (mode) {
        Routine.PROFILE_SILENT -> getString(R.string.sound_mode_silent)
        Routine.PROFILE_VIBRATE -> getString(R.string.sound_mode_vibrate)
        else -> getString(R.string.sound_mode_ring)
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

    private companion object {
        const val STATE_AWAITING_SOUND_ACCESS = "awaiting_sound_access"
        const val STATE_AWAITING_LOCATION_ACCESS = "awaiting_location_access"
    }
}
