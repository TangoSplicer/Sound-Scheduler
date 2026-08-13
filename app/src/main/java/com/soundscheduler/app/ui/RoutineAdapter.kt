package com.soundscheduler.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.soundscheduler.app.R
import com.soundscheduler.app.data.Routine
import java.text.DateFormat
import java.util.Date

class RoutineAdapter(
    private val context: Context,
    private val onDelete: (Routine) -> Unit
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
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteButton)

        titleText.text = routine.title
        detailText.text = routineDetail(routine)
        deleteButton.setOnClickListener { onDelete(routine) }
        return view
    }

    private fun routineDetail(routine: Routine): String {
        if (routine.isCompleted) return context.getString(R.string.one_time) + " · Completed"

        val time = routine.time?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
        } ?: "Unscheduled"
        val repetition = when (routine.recurrence) {
            Routine.RECURRENCE_DAILY -> context.getString(R.string.daily)
            Routine.RECURRENCE_WEEKLY -> context.getString(R.string.weekly)
            Routine.RECURRENCE_MONTHLY -> context.getString(R.string.monthly)
            else -> context.getString(R.string.one_time)
        }
        return context.getString(R.string.routine_time_format, time, repetition)
    }
}
