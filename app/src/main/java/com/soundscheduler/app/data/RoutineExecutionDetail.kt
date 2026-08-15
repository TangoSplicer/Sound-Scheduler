package com.soundscheduler.app.data

import androidx.room.Embedded

/** View-only local join used by the Activity screen. */
data class RoutineExecutionDetail(
    @Embedded val execution: RoutineExecution,
    val routineTitle: String
)
