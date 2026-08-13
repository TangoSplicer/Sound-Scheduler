package com.soundscheduler.app.utils

import com.soundscheduler.app.data.Routine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RoutineAlarmSchedulerTest {
    @Test
    fun `future one-time routine retains its requested trigger`() {
        val now = fixedTime(2026, Calendar.AUGUST, 13, 9, 0)
        val future = fixedTime(2026, Calendar.AUGUST, 13, 10, 0)
        val routine = timeRoutine(time = future)

        assertEquals(future, RoutineAlarmScheduler.nextTriggerAt(routine, now))
    }

    @Test
    fun `expired one-time routine has no next trigger`() {
        val now = fixedTime(2026, Calendar.AUGUST, 13, 9, 0)
        val expired = fixedTime(2026, Calendar.AUGUST, 13, 8, 0)
        val routine = timeRoutine(time = expired)

        assertNull(RoutineAlarmScheduler.nextTriggerAt(routine, now))
    }

    @Test
    fun `daily routine advances to the next future day`() {
        val now = fixedTime(2026, Calendar.AUGUST, 13, 9, 0)
        val yesterday = fixedTime(2026, Calendar.AUGUST, 12, 8, 0)
        val routine = timeRoutine(time = yesterday, recurrence = Routine.RECURRENCE_DAILY)

        val result = RoutineAlarmScheduler.nextTriggerAt(routine, now)

        assertTrue(result != null && result > now)
        assertEquals(
            fixedTime(2026, Calendar.AUGUST, 14, 8, 0),
            result
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `time routine requires a positive time`() {
        Routine(
            title = "Invalid routine",
            type = Routine.TYPE_TIME,
            time = 0L
        )
    }

    private fun timeRoutine(time: Long, recurrence: String? = null): Routine = Routine(
        id = 1,
        title = "Focus session",
        type = Routine.TYPE_TIME,
        time = time,
        recurrence = recurrence
    )

    private fun fixedTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
