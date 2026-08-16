package com.soundscheduler.app.utils

import com.soundscheduler.app.data.RoutineExecutionDetail
import java.util.Calendar

object ContextSuggestionEngine {
    data class Suggestion(
        val title: String,
        val suggestedMode: String,
        val reason: String
    )

    fun analyzeHistory(details: List<RoutineExecutionDetail>): List<Suggestion> {
        // Group manual or recurring applications by hour of day
        val hourCounts = mutableMapOf<Int, MutableMap<String, Int>> ()
        
        for (d in details) {
            val cal = Calendar.getInstance().apply { timeInMillis = d.execution.occurredAtMillis }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val mode = d.execution.requestedMode
            
            val modeMap = hourCounts.getOrPut(hour) { mutableMapOf() }
            modeMap[mode] = (modeMap[mode] ?: 0) + 1
        }

        val suggestions = mutableListOf<Suggestion>()
        for ((hour, modes) in hourCounts) {
            for ((mode, count) in modes) {
                if (count >= 3) { // 3 or more occurrences at the same hour
                    val timeStr = String.format("%02d:00", hour)
                    suggestions.add(
                        Suggestion(
                            title = "Frequent $mode at $timeStr",
                            suggestedMode = mode,
                            reason = "You frequently switch to $mode around $timeStr. Create a daily routine?"
                        )
                    )
                }
            }
        }
        return suggestions.take(3)
    }
}
