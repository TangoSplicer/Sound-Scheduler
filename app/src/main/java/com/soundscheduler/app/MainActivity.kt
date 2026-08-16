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
import android.widget.CheckBox
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
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.AutomationControlRepository
import com.soundscheduler.app.data.AutomationState
import com.soundscheduler.app.data.AutomationStateRepository
import com.soundscheduler.app.services.SoundModeExecutionService
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
    private lateinit var automationTitleTextView: TextView
    private lateinit var automationDetailTextView: TextView
    private lateinit var automationPrimaryButton: MaterialButton
    private var automationState: AutomationState? = null
    private var activeRoutineCount = 0
    private var currentRoutines: List<Routine> = emptyList()
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
        AutomationStateRepository.ensureState(this)
        routineListView = findViewById(R.id.routineListView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        scheduleStatusTextView = findViewById(R.id.scheduleStatusTextView)
        soundAccessButton = findViewById(R.id.soundAccessButton)
        automationTitleTextView = findViewById(R.id.automationTitleTextView)
        automationDetailTextView = findViewById(R.id.automationDetailTextView)
        automationPrimaryButton = findViewById(R.id.automationPrimaryButton)
        automationPrimaryButton.setOnClickListener { handleAutomationPrimaryAction() }
        findViewById<MaterialButton>(R.id.viewActivityButton).setOnClickListener {
            startActivity(Intent(this, ExecutionLogActivity::class.java))
        }
        routineAdapter = RoutineAdapter(
            this,
            ::confirmRoutineDeletion,
            ::setRoutineEnabled,
            ::showEditRoutineDialog,
            ::showDuplicateRoutineDialog
        )
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
            currentRoutines = routines
            routineAdapter.submitList(routines)
            activeRoutineCount = routines.count { it.isEnabled }
            activeTimeRoutineCount = routines.count { it.isEnabled && it.type == Routine.TYPE_TIME }
            activeLocationRoutineCount = routines.count { it.isEnabled && it.type == Routine.TYPE_LOCATION }
            SoundModeExecutionService.syncAutomationLifecycle(
                this,
                activeRoutineCount > 0 && automationState?.isPaused != true
            )
            renderAutomationCard()
            routineListView.visibility = if (routines.isEmpty()) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (routines.isEmpty()) View.VISIBLE else View.GONE
            updateScheduleStatus()
        })
        AppDatabase.getDatabase(this).automationStateDao().observeState().observe(this, Observer { state ->
            automationState = state
            SoundModeExecutionService.syncAutomationLifecycle(
                this,
                activeRoutineCount > 0 && state?.isPaused != true
            )
            renderAutomationCard()
        })
    }

    override fun onResume() {
        super.onResume()
        val returnedFromSoundAccessSettings = awaitingSoundAccessResult
        val returnedFromLocationAccessSettings = awaitingLocationAccessResult
        val hasSoundAccess = SoundModeController.hasNotificationPolicyAccess(this)
        val hasLocationAccess = LocationRoutineManager.hasRequiredLocationAccess(this)

        if ((hasSoundAccess || hasLocationAccess) && automationState?.isPaused != true) {
            RoutineRescheduler.rescheduleActiveRoutines(this)
        }
        SoundModeExecutionService.syncAutomationLifecycle(
            this,
            activeRoutineCount > 0 && automationState?.isPaused != true
        )
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

    private fun renderAutomationCard() {
        val state = automationState
        when {
            state?.isPaused == true -> {
                automationTitleTextView.setText(R.string.automation_paused_title)
                val until = state.pauseUntilMillis
                if (until != null && until > System.currentTimeMillis()) {
                    val timeStr = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(until))
                    automationDetailTextView.text = getString(R.string.automation_paused_until_format, timeStr)
                } else {
                    automationDetailTextView.setText(R.string.automation_paused_detail)
                }
                automationPrimaryButton.setText(R.string.resume_automation)
            }
            activeRoutineCount == 0 -> {
                automationTitleTextView.setText(R.string.automation_off_title)
                automationDetailTextView.setText(R.string.automation_off_detail)
                automationPrimaryButton.setText(R.string.create_routine)
            }
            state?.lastStateCode == AutomationState.STATE_REARM_REQUIRED ||
                !SoundModeExecutionService.isForegroundReady() -> {
                automationTitleTextView.setText(R.string.automation_rearm_title)
                automationDetailTextView.setText(R.string.automation_rearm_detail)
                automationPrimaryButton.setText(R.string.rearm_automation)
            }
            else -> {
                automationTitleTextView.setText(R.string.automation_active_title)
                automationDetailTextView.text = getString(
                    R.string.automation_active_detail_format,
                    activeRoutineCount
                )
                automationPrimaryButton.setText(R.string.pause_all)
            }
        }
    }

    private fun handleAutomationPrimaryAction() {
        when {
            automationState?.isPaused == true -> resumeAutomation()
            activeRoutineCount == 0 -> showCreateRoutineDialog()
            automationState?.lastStateCode == AutomationState.STATE_REARM_REQUIRED ||
                !SoundModeExecutionService.isForegroundReady() -> {
                SoundModeExecutionService.syncAutomationLifecycle(this, true)
            }
            else -> showPauseOptionsDialog()
        }
    }

    private fun showPauseOptionsDialog() {
        val options = arrayOf(
            getString(R.string.pause_1_hour),
            getString(R.string.pause_4_hours),
            getString(R.string.pause_until_morning),
            getString(R.string.pause_indefinitely)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.pause_all_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pauseFor(1 * 60 * 60 * 1000L)
                    1 -> pauseFor(4 * 60 * 60 * 1000L)
                    2 -> pauseUntilMorning()
                    3 -> confirmPauseAll()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pauseFor(durationMillis: Long) {
        val until = System.currentTimeMillis() + durationMillis
        AutomationControlRepository.pauseUntil(this, until)
    }

    private fun pauseUntilMorning() {
        val morning = Calendar.getInstance().apply {
            if (get(Calendar.HOUR_OF_DAY) >= 7) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        AutomationControlRepository.pauseUntil(this, morning.timeInMillis)
    }

    private fun confirmPauseAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pause_all_title)
            .setMessage(R.string.pause_all_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.pause_all) { _, _ ->
                AutomationControlRepository.pauseAll(this) {
                    runOnUiThread {
                        SoundModeExecutionService.syncAutomationLifecycle(this, false)
                    }
                }
            }
            .show()
    }

    private fun resumeAutomation() {
        AutomationControlRepository.resumeAll(this) {
            runOnUiThread {
                SoundModeExecutionService.syncAutomationLifecycle(this, true)
            }
        }
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

    private fun showEditRoutineDialog(routine: Routine) {
        showCreateRoutineDialog(existing = routine)
    }

    private fun showDuplicateRoutineDialog(routine: Routine) {
        showCreateRoutineDialog(prefill = routine)
    }

    private fun showCreateRoutineDialog(existing: Routine? = null, prefill: Routine? = null) {
        val content = LayoutInflater.from(this).inflate(R.layout.dialog_create_routine, null)
        val data = existing ?: prefill
        val nameInput = content.findViewById<TextInputEditText>(R.id.routineNameEditText)
        val soundModeSpinner = content.findViewById<Spinner>(R.id.soundModeSpinner)
        val routineTypeRadioGroup = content.findViewById<RadioGroup>(R.id.routineTypeRadioGroup)
        val timeSection = content.findViewById<LinearLayout>(R.id.timeRoutineSection)
        val locationSection = content.findViewById<LinearLayout>(R.id.locationRoutineSection)
        val chargingSection = content.findViewById<LinearLayout>(R.id.chargingRoutineSection)
        val timePicker = content.findViewById<TimePicker>(R.id.routineTimePicker)
        val recurrenceSpinner = content.findViewById<Spinner>(R.id.recurrenceSpinner)
        val captureLocationButton = content.findViewById<MaterialButton>(R.id.captureLocationButton)
        val locationLabelInput = content.findViewById<TextInputEditText>(R.id.locationLabelEditText)
        val radiusSpinner = content.findViewById<Spinner>(R.id.locationRadiusSpinner)
        val transitionSpinner = content.findViewById<Spinner>(R.id.locationTransitionSpinner)
        val chargingTransitionSpinner = content.findViewById<Spinner>(R.id.chargingTransitionSpinner)
        val weekdaySection = content.findViewById<LinearLayout>(R.id.weekdaySelectorSection)
        val dayCheckBoxes = listOf(
            content.findViewById<CheckBox>(R.id.dayMon),
            content.findViewById<CheckBox>(R.id.dayTue),
            content.findViewById<CheckBox>(R.id.dayWed),
            content.findViewById<CheckBox>(R.id.dayThu),
            content.findViewById<CheckBox>(R.id.dayFri),
            content.findViewById<CheckBox>(R.id.daySat),
            content.findViewById<CheckBox>(R.id.daySun)
        )
        var capturedLocation: Location? = existing?.takeIf { it.hasUsableLocation() }?.let { routine ->
            Location("stored_routine").apply {
                latitude = routine.latitude ?: 0.0
                longitude = routine.longitude ?: 0.0
            }
        }

        timePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(this))
        radiusSpinner.setSelection(1)
        val defaults = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timePicker.hour = defaults.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = defaults.get(Calendar.MINUTE)

        val updateWeekdayVisibility = {
            val recurrence = recurrenceForPosition(recurrenceSpinner.selectedItemPosition)
            weekdaySection.visibility = if (recurrence == Routine.RECURRENCE_WEEKLY) View.VISIBLE else View.GONE
        }
        recurrenceSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateWeekdayVisibility()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        data?.let { routine ->
            nameInput.setText(routine.title)
            soundModeSpinner.setSelection(soundModePosition(routine.targetSoundMode()))
            recurrenceSpinner.setSelection(recurrencePosition(routine.recurrence))
            when (routine.type) {
                Routine.TYPE_LOCATION -> {
                    routineTypeRadioGroup.check(R.id.locationRoutineRadioButton)
                    timeSection.visibility = View.GONE
                    locationSection.visibility = View.VISIBLE
                    chargingSection.visibility = View.GONE
                    locationLabelInput.setText(routine.location.orEmpty())
                    radiusSpinner.setSelection(radiusPosition(routine.radiusMeters))
                    transitionSpinner.setSelection(transitionPosition(routine.locationTransition))
                }
                Routine.TYPE_CHARGING -> {
                    routineTypeRadioGroup.check(R.id.chargingRoutineRadioButton)
                    timeSection.visibility = View.GONE
                    locationSection.visibility = View.GONE
                    chargingSection.visibility = View.VISIBLE
                    chargingTransitionSpinner.setSelection(chargingTransitionPosition(routine.chargingTransition))
                }
                else -> {
                    routineTypeRadioGroup.check(R.id.timeRoutineRadioButton)
                    timeSection.visibility = View.VISIBLE
                    locationSection.visibility = View.GONE
                    chargingSection.visibility = View.GONE
                    routine.time?.let { scheduledAt ->
                        Calendar.getInstance().apply {
                            timeInMillis = scheduledAt
                            timePicker.hour = get(Calendar.HOUR_OF_DAY)
                            timePicker.minute = get(Calendar.MINUTE)
                        }
                    }
                }
            }
            val days = Routine.parseDays(routine.daysOfWeek)
            dayCheckBoxes.forEachIndexed { index, checkBox ->
                checkBox.isChecked = days.contains(index + 1)
            }
            updateWeekdayVisibility()
        }

        routineTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val useLocation = checkedId == R.id.locationRoutineRadioButton
            val useCharging = checkedId == R.id.chargingRoutineRadioButton
            timeSection.visibility = if (!useLocation && !useCharging) View.VISIBLE else View.GONE
            locationSection.visibility = if (useLocation) View.VISIBLE else View.GONE
            chargingSection.visibility = if (useCharging) View.VISIBLE else View.GONE
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
            .setTitle(if (existing == null) R.string.create_routine_title else R.string.edit_routine)
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
                if (routineTypeRadioGroup.checkedRadioButtonId == R.id.chargingRoutineRadioButton) {
                    val routine = Routine(
                        title = title,
                        type = Routine.TYPE_CHARGING,
                        chargingTransition = chargingTransitionForPosition(
                            chargingTransitionSpinner.selectedItemPosition
                        ),
                        recurrence = Routine.RECURRENCE_DAILY,
                        soundProfile = targetMode
                    )
                    saveRoutine(existing, routine, dialog)
                    return@setOnClickListener
                }
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
                    saveRoutine(existing, routine, dialog)
                    return@setOnClickListener
                }

                val recurrence = recurrenceForPosition(recurrenceSpinner.selectedItemPosition)
                val selectedDays = dayCheckBoxes.mapIndexedNotNull { index, checkBox ->
                    if (checkBox.isChecked) index + 1 else null
                }.toSet()
                val daysOfWeek = if (recurrence == Routine.RECURRENCE_WEEKLY) Routine.formatDays(selectedDays) else null

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
                    daysOfWeek = daysOfWeek,
                    soundProfile = targetMode
                )

                saveRoutine(existing, routine, dialog)
            }
        }
        dialog.show()
    }

    private fun saveRoutine(
        existing: Routine?,
        draft: Routine,
        dialog: AlertDialog,
        bypassConflictWarning: Boolean = false
    ) {
        val wasGloballyPaused = automationState?.isPaused == true
        val savedRoutine = if (existing == null) {
            draft.copy(
                isEnabled = !wasGloballyPaused,
                wasEnabledBeforeGlobalPause = wasGloballyPaused
            )
        } else {
            draft.copy(
                id = existing.id,
                isCompleted = existing.isCompleted,
                isEnabled = existing.isEnabled,
                lastAttemptAtMillis = existing.lastAttemptAtMillis,
                lastOutcomeAtMillis = existing.lastOutcomeAtMillis,
                lastOutcomeCode = existing.lastOutcomeCode,
                lastObservedMode = existing.lastObservedMode,
                lastOutcomeDetailCode = existing.lastOutcomeDetailCode,
                lastExecutionId = existing.lastExecutionId,
                wasEnabledBeforeGlobalPause = existing.wasEnabledBeforeGlobalPause
            )
        }

        val conflictingRoutine = savedRoutine.takeIf {
            !bypassConflictWarning && it.isEnabled && it.type == Routine.TYPE_TIME && it.time != null
        }?.let { candidate ->
            currentRoutines.firstOrNull { other ->
                other.id != candidate.id &&
                    other.isEnabled &&
                    !other.isCompleted &&
                    other.type == Routine.TYPE_TIME &&
                    other.time == candidate.time &&
                    other.targetSoundMode() != candidate.targetSoundMode()
            }
        }
        if (conflictingRoutine != null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.routine_conflict_title)
                .setMessage(getString(R.string.routine_conflict_message, conflictingRoutine.title))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save_anyway) { _, _ ->
                    saveRoutine(existing, draft, dialog, bypassConflictWarning = true)
                }
                .show()
            return
        }

        existing?.let { oldRoutine ->
            if (oldRoutine.type == Routine.TYPE_LOCATION) {
                LocationRoutineManager.removeGeofence(this, oldRoutine)
            } else {
                RoutineAlarmScheduler.cancel(this, oldRoutine)
            }
        }

        val onSaved: (Routine) -> Unit = { persistedRoutine ->
            runOnUiThread {
                if (persistedRoutine.isEnabled) {
                    if (persistedRoutine.type == Routine.TYPE_LOCATION) {
                        LocationRoutineManager.refreshActiveGeofences(this) { registered ->
                            if (!registered && !LocationRoutineManager.hasRequiredLocationAccess(this)) {
                                runOnUiThread { showLocationAccessGuidance() }
                            }
                        }
                    } else {
                        val result = RoutineAlarmScheduler.schedule(this, persistedRoutine)
                        if (SoundModeController.hasNotificationPolicyAccess(this) && result?.exact != true) {
                            showExactAlarmGuidanceIfNeeded()
                        }
                    }
                }
                if (!SoundModeController.hasNotificationPolicyAccess(this)) {
                    showSoundAccessGuidance()
                } else {
                    requestNotificationPermissionIfNeeded()
                }
                Toast.makeText(
                    this,
                    if (existing == null) R.string.routine_created_exact else R.string.routine_updated,
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
        }

        if (existing == null) {
            viewModel.insert(savedRoutine, onSaved)
        } else {
            viewModel.update(savedRoutine) { onSaved(savedRoutine) }
        }
    }

    private fun setRoutineEnabled(routine: Routine, enabled: Boolean) {
        if (!enabled) {
            when (routine.type) {
                Routine.TYPE_LOCATION -> LocationRoutineManager.removeGeofence(this, routine)
                Routine.TYPE_TIME -> RoutineAlarmScheduler.cancel(this, routine)
            }
            viewModel.setEnabled(routine.id, false)
            Toast.makeText(this, R.string.routine_paused, Toast.LENGTH_SHORT).show()
            return
        }

        val resumedRoutine = routine.copy(isEnabled = true)
        viewModel.setEnabled(routine.id, true) {
            runOnUiThread {
                when (resumedRoutine.type) {
                    Routine.TYPE_LOCATION -> LocationRoutineManager.refreshActiveGeofences(this) { registered ->
                        if (!registered && !LocationRoutineManager.hasRequiredLocationAccess(this)) {
                            runOnUiThread { showLocationAccessGuidance() }
                        }
                    }
                    Routine.TYPE_TIME -> {
                        val result = RoutineAlarmScheduler.schedule(this, resumedRoutine)
                        if (SoundModeController.hasNotificationPolicyAccess(this) && result?.exact != true) {
                            showExactAlarmGuidanceIfNeeded()
                        }
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

    private fun recurrencePosition(recurrence: String?): Int = when (recurrence) {
        Routine.RECURRENCE_DAILY -> 1
        Routine.RECURRENCE_WEEKLY -> 2
        Routine.RECURRENCE_MONTHLY -> 3
        else -> 0
    }

    private fun soundModePosition(mode: String): Int = when (mode) {
        Routine.PROFILE_VIBRATE -> 1
        Routine.PROFILE_SILENT -> 2
        else -> 0
    }

    private fun radiusPosition(radiusMeters: Int?): Int = when (radiusMeters) {
        100 -> 0
        250 -> 2
        500 -> 3
        else -> 1
    }

    private fun transitionPosition(transition: String?): Int = when (transition) {
        Routine.LOCATION_TRANSITION_EXIT -> 1
        else -> 0
    }

    private fun chargingTransitionPosition(transition: String?): Int = when (transition) {
        Routine.CHARGING_TRANSITION_DISCONNECTED -> 1
        else -> 0
    }

    private fun chargingTransitionForPosition(position: Int): String = when (position) {
        1 -> Routine.CHARGING_TRANSITION_DISCONNECTED
        else -> Routine.CHARGING_TRANSITION_CONNECTED
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
