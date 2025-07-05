package com.soundscheduler.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.soundscheduler.app.R
import com.soundscheduler.app.data.Routine

class RoutineAdapter(private val context: Context, private val routines: List<Routine>) : BaseAdapter() {

    override fun getCount(): Int = routines.size

    override fun getItem(position: Int): Any = routines[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_routine, parent, false)
        val routine = routines[position]

        val titleText = view.findViewById<TextView>(R.id.routineTitle)
        val typeText = view.findViewById<TextView>(R.id.routineType)

        titleText.text = routine.title
        typeText.text = "Type: ${routine.type} | Recurrence: ${routine.recurrence ?: "None"}"

        return view
    }
}