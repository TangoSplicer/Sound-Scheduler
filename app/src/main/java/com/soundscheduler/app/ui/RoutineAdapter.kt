package com.soundscheduler.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.soundscheduler.app.R
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.data.RoutineExecution
import java.text.DateFormat
import java.util.Date

class RoutineAdapter(
    private val context: Context,
    private val onDelete: (Routine) -> Unit,
    private val onEnabledChange: (Routine, Boolean) -> Unit,
    private val onEdit: (Routine) -> Unit
) : BaseAdapter() {
    private var routines: List<Routine> = emptyList()

    fun submitList(updatedRoutines: List<Routine>) {
        routines = updatedRoutines.toList()
        notifyDataSetChanged()
    }

    override fun getCount(): Int = routines.size

    override fun getItem(position: Int): Routine = routines[position]

    override fun getItemId(position: Int): Long = routines[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_routine, parent, false)
        val routine = getItem(position)

        val titleText = view.findViewById<TextView>(R.id.routineTitle)
        val detailText = view.findViewById<TextView>(R.id.routineType)
        val lastRunText = view.findViewById<TextView>(R.id.routineLastRun)
        val enabledSwitch = view.findViewById<SwitchMaterial>(R.id.routineEnabledSwitch)
        val editButton = view.findViewById<ImageButton>(R.id.editButton)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteButton)

        titleText.text = routine.title
        detailText.text = routineDetail(routine)
        lastRunText.text = lastRunDetail(routine)
        enabledSwitch.setOnCheckedChangeListener(null)
        enabledSwitch.isChecked = routine.isEnabled
        enabledSwitch.contentDescription = routine.title + ": " + if (routine.isEnabled) {
            context.getString(R.string.routine_enabled)
        } else {
            context.getString(R.string.routine_paused)
        }
        enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != routine.isEnabled) onEnabledChange(routine, isChecked)
        }
        editButton.setOnClickListener { onEdit(routine) }
        deleteButton.setOnClickListener { onDelete(routine) }
        return view
    }

    private fun routineDetail(routine: Routine): String {
        val targetMode = soundModeLabel(routine.targetSoundMode())
        if (routine.type == Routine.TYPE_CHARGING) {
            val chargingDetail = context.getString(
                R.string.charging_routine_detail_format,
                targetMode,
                chargingTransitionLabel(routine.chargingTransition)
            )
            return when {
                !routine.isEnabled -> "$chargingDetail · ${context.getString(R.string.routine_paused)}"
                routine.isCompleted -> "$chargingDetail · ${context.getString(R.string.completed)}"
                else -> chargingDetail
            }
        }
        if (routine.type == Routine.TYPE_LOCATION) {
            val locationDetail = context.getString(
                R.string.location_routine_detail_format,
                targetMode,
                routine.location.orEmpty().ifBlank { context.getString(R.string.unscheduled) },
                locationTransitionLabel(routine.locationTransition),
                routine.radiusMeters ?: Routine.DEFAULT_LOCATION_RADIUS_METERS
            )
            return when {
                !routine.isEnabled -> "$locationDetail · ${context.getString(R.string.routine_paused)}"
                routine.isCompleted -> "$locationDetail · ${context.getString(R.string.completed)}"
                else -> locationDetail
            }
        }
        if (!routine.isEnabled) {
            return context.getString(
                R.string.routine_mode_time_format,
                targetMode,
                context.getString(R.string.routine_paused),
                context.getString(R.string.one_time)
            )
        }
        if (routine.isCompleted) {
            return context.getString(
                R.string.routine_mode_time_format,
                targetMode,
                context.getString(R.string.one_time),
                context.getString(R.string.completed)
            )
        }

        val time = routine.time?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
        } ?: context.getString(R.string.unscheduled)
        val repetition = when (routine.recurrence) {
            Routine.RECURRENCE_DAILY -> context.getString(R.string.daily)
            Routine.RECURRENCE_WEEKLY -> context.getString(R.string.weekly)
            Routine.RECURRENCE_MONTHLY -> context.getString(R.string.monthly)
            else -> context.getString(R.string.one_time)
        }
        return context.getString(R.string.routine_mode_time_format, targetMode, time, repetition)
    }

    private fun lastRunDetail(routine: Routine): String {
        val outcome = when (routine.lastOutcomeCode) {
            RoutineExecution.OUTCOME_APPLIED -> context.getString(
                R.string.execution_applied_format,
                soundModeLabel(routine.lastObservedMode ?: routine.targetSoundMode())
            )
            RoutineExecution.OUTCOME_MODE_REJECTED -> context.getString(
                R.string.execution_rejected_format,
                soundModeLabel(routine.lastObservedMode ?: Routine.PROFILE_RING)
            )
            RoutineExecution.OUTCOME_ACCESS_REQUIRED -> context.getString(R.string.execution_access_required)
            RoutineExecution.OUTCOME_AUTOMATION_REARM_REQUIRED ->
                context.getString(R.string.execution_rearm_required)
            RoutineExecution.OUTCOME_EXACT_ALARM_DEFERRED ->
                context.getString(R.string.execution_exact_alarm_deferred)
            RoutineExecution.OUTCOME_LOCATION_ACCESS_REQUIRED ->
                context.getString(R.string.execution_location_access_required)
            RoutineExecution.OUTCOME_LOCATION_UNAVAILABLE ->
                context.getString(R.string.execution_location_unavailable)
            RoutineExecution.OUTCOME_PAUSED -> context.getString(R.string.execution_paused)
            RoutineExecution.OUTCOME_INVALID_CONFIGURATION ->
                context.getString(R.string.execution_invalid_configuration)
            else -> context.getString(R.string.last_run_not_yet)
        }
        val time = routine.lastOutcomeAtMillis?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
        }
        return if (time == null) {
            "${context.getString(R.string.last_run)}: $outcome"
        } else {
            "${context.getString(R.string.last_run)}: $outcome · $time"
        }
    }

    private fun chargingTransitionLabel(transition: String?): String = when (transition) {
        Routine.CHARGING_TRANSITION_DISCONNECTED -> context.getString(R.string.charging_disconnected)
        else -> context.getString(R.string.charging_connected)
    }

    private fun locationTransitionLabel(transition: String?): String = when (transition) {
        Routine.LOCATION_TRANSITION_EXIT -> context.getString(R.string.transition_exit)
        else -> context.getString(R.string.transition_enter)
    }

    private fun soundModeLabel(mode: String): String = when (mode) {
        Routine.PROFILE_SILENT -> context.getString(R.string.sound_mode_silent)
        Routine.PROFILE_VIBRATE -> context.getString(R.string.sound_mode_vibrate)
        else -> context.getString(R.string.sound_mode_ring)
    }
}
