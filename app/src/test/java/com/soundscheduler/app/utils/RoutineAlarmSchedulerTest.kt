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

    @Test
    fun `new routine defaults to ring mode`() {
        val routine = timeRoutine(time = fixedTime(2026, Calendar.AUGUST, 13, 10, 0))

        assertEquals(Routine.PROFILE_RING, routine.targetSoundMode())
    }

    @Test
    fun `new routine is enabled and can be paused without changing its target`() {
        val routine = timeRoutine(time = fixedTime(2026, Calendar.AUGUST, 13, 10, 0))
        val pausedRoutine = routine.copy(isEnabled = false)

        assertTrue(routine.isEnabled)
        assertTrue(!pausedRoutine.isEnabled)
        assertEquals(routine.targetSoundMode(), pausedRoutine.targetSoundMode())
    }

    @Test
    fun `each supported sound mode remains its own target`() {
        val time = fixedTime(2026, Calendar.AUGUST, 13, 10, 0)

        assertEquals(Routine.PROFILE_RING, timeRoutine(time, Routine.PROFILE_RING).targetSoundMode())
        assertEquals(Routine.PROFILE_VIBRATE, timeRoutine(time, Routine.PROFILE_VIBRATE).targetSoundMode())
        assertEquals(Routine.PROFILE_SILENT, timeRoutine(time, Routine.PROFILE_SILENT).targetSoundMode())
    }

    @Test
    fun `legacy sound profiles normalize to ring`() {
        val time = fixedTime(2026, Calendar.AUGUST, 13, 10, 0)

        assertEquals(Routine.PROFILE_RING, timeRoutine(time, Routine.PROFILE_NORMAL).targetSoundMode())
        assertEquals(Routine.PROFILE_RING, timeRoutine(time, Routine.PROFILE_CUSTOM).targetSoundMode())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `time routine requires a positive time`() {
        Routine(
            title = "Invalid routine",
            type = Routine.TYPE_TIME,
            time = 0L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `routine rejects unknown sound mode`() {
        timeRoutine(
            time = fixedTime(2026, Calendar.AUGUST, 13, 10, 0),
            soundProfile = "unsupported"
        )
    }

    private fun timeRoutine(
        time: Long,
        soundProfile: String = Routine.PROFILE_RING,
        recurrence: String? = null
    ): Routine = Routine(
        id = 1,
        title = "Focus session",
        type = Routine.TYPE_TIME,
        time = time,
        recurrence = recurrence,
        soundProfile = soundProfile
    )

    @Test
    fun `two enabled daily routines remain valid after an app update`() {
        val now = fixedTime(2026, Calendar.AUGUST, 17, 9, 0)
        val morning = Routine(
            id = 1,
            title = "Morning ring",
            type = Routine.TYPE_TIME,
            time = fixedTime(2026, Calendar.AUGUST, 17, 8, 0),
            recurrence = Routine.RECURRENCE_DAILY,
            soundProfile = Routine.PROFILE_RING,
            isEnabled = true
        )
        val evening = Routine(
            id = 2,
            title = "Evening vibrate",
            type = Routine.TYPE_TIME,
            time = fixedTime(2026, Calendar.AUGUST, 17, 18, 0),
            recurrence = Routine.RECURRENCE_DAILY,
            soundProfile = Routine.PROFILE_VIBRATE,
            isEnabled = true
        )

        assertTrue(morning.isEnabled)
        assertTrue(evening.isEnabled)
        assertEquals(Routine.PROFILE_RING, morning.targetSoundMode())
        assertEquals(Routine.PROFILE_VIBRATE, evening.targetSoundMode())
        assertEquals(fixedTime(2026, Calendar.AUGUST, 18, 8, 0), RoutineAlarmScheduler.nextTriggerAt(morning, now))
        assertEquals(fixedTime(2026, Calendar.AUGUST, 17, 18, 0), RoutineAlarmScheduler.nextTriggerAt(evening, now))
    }

    @Test
    fun `weekly routine with specific days returns next allowed day`() {
        // Thursday, August 13, 2026
        val now = fixedTime(2026, Calendar.AUGUST, 13, 9, 0)
        
        // Schedule for Friday (internal 5) and Monday (internal 1)
        val routine = timeRoutine(
            time = fixedTime(2026, Calendar.AUGUST, 13, 8, 0),
            recurrence = Routine.RECURRENCE_WEEKLY
        ).copy(daysOfWeek = "1,5")

        // From Thursday 9am, next should be Friday 8am
        val next = RoutineAlarmScheduler.nextTriggerAt(routine, now)
        assertEquals(fixedTime(2026, Calendar.AUGUST, 14, 8, 0), next)
        
        // From Friday 8am, next should be next Monday 8am
        val nextFromFri = RoutineAlarmScheduler.nextTriggerAt(routine, next!!)
        assertEquals(fixedTime(2026, Calendar.AUGUST, 17, 8, 0), nextFromFri)
    }

    @Test
    fun `weekly routine without specific days advances by one week`() {
        val now = fixedTime(2026, Calendar.AUGUST, 13, 9, 0)
        val lastWeek = fixedTime(2026, Calendar.AUGUST, 6, 8, 0)
        val routine = timeRoutine(time = lastWeek, recurrence = Routine.RECURRENCE_WEEKLY)

        val next = RoutineAlarmScheduler.nextTriggerAt(routine, now)
        assertEquals(fixedTime(2026, Calendar.AUGUST, 20, 8, 0), next)
    }

    private fun fixedTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
