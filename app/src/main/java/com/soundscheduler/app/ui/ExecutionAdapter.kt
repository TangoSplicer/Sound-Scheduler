package com.soundscheduler.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.soundscheduler.app.R
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.data.RoutineExecution
import com.soundscheduler.app.data.RoutineExecutionDetail
import java.text.DateFormat
import java.util.Date

class ExecutionAdapter(private val context: Context) : BaseAdapter() {
    private var executions: List<RoutineExecutionDetail> = emptyList()

    fun submitList(updatedExecutions: List<RoutineExecutionDetail>) {
        executions = updatedExecutions.toList()
        notifyDataSetChanged()
    }

    override fun getCount(): Int = executions.size
    override fun getItem(position: Int): RoutineExecutionDetail = executions[position]
    override fun getItemId(position: Int): Long = executions[position].execution.id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_execution, parent, false)
        val detail = getItem(position)
        val execution = detail.execution

        view.findViewById<TextView>(R.id.executionTitle).text = detail.routineTitle
        view.findViewById<TextView>(R.id.executionTimestamp).text = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(Date(execution.occurredAtMillis))
        view.findViewById<TextView>(R.id.executionOutcome).text = outcomeText(execution)
        view.contentDescription = "${detail.routineTitle}. ${outcomeText(execution)}"
        return view
    }

    private fun outcomeText(execution: RoutineExecution): String {
        return when (execution.outcomeCode) {
            RoutineExecution.OUTCOME_APPLIED -> context.getString(
                R.string.execution_applied_format,
                modeLabel(execution.observedMode ?: execution.requestedMode)
            )
            RoutineExecution.OUTCOME_MODE_REJECTED -> context.getString(
                R.string.execution_rejected_format,
                modeLabel(execution.observedMode ?: Routine.PROFILE_RING)
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
            else -> context.getString(R.string.execution_invalid_configuration)
        }
    }

    private fun modeLabel(mode: String): String = when (mode) {
        Routine.PROFILE_SILENT -> context.getString(R.string.sound_mode_silent)
        Routine.PROFILE_VIBRATE -> context.getString(R.string.sound_mode_vibrate)
        else -> context.getString(R.string.sound_mode_ring)
    }
}
