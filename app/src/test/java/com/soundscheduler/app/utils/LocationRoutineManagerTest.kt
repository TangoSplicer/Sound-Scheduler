package com.soundscheduler.app.utils

import com.soundscheduler.app.data.Routine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRoutineManagerTest {
    @Test
    fun `location routine with supported coordinates radius and transition is usable`() {
        val routine = locationRoutine()

        assertTrue(routine.hasUsableLocation())
    }

    @Test
    fun `location routine with invalid latitude is not usable`() {
        val routine = locationRoutine().copy(latitude = 91.0)

        assertFalse(routine.hasUsableLocation())
    }

    @Test
    fun `location routine with unsupported radius is not usable`() {
        val routine = locationRoutine().copy(radiusMeters = 50)

        assertFalse(routine.hasUsableLocation())
    }

    @Test
    fun `location routine with unsupported transition is not usable`() {
        val routine = locationRoutine().copy(locationTransition = "dwell")

        assertFalse(routine.hasUsableLocation())
    }

    @Test
    fun `request IDs round trip only for the app geofence format`() {
        val requestId = LocationRoutineManager.requestIdFor(42)

        assertEquals("sound_location_42", requestId)
        assertEquals(42, LocationRoutineManager.routineIdFromRequestId(requestId))
        assertNull(LocationRoutineManager.routineIdFromRequestId("other_location_42"))
        assertNull(LocationRoutineManager.routineIdFromRequestId("sound_location_bad"))
    }

    private fun locationRoutine(): Routine = Routine(
        id = 42,
        title = "Home arrival",
        type = Routine.TYPE_LOCATION,
        location = "Home",
        latitude = 51.5074,
        longitude = -0.1278,
        radiusMeters = Routine.DEFAULT_LOCATION_RADIUS_METERS,
        locationTransition = Routine.LOCATION_TRANSITION_ENTER,
        recurrence = Routine.RECURRENCE_DAILY,
        soundProfile = Routine.PROFILE_VIBRATE
    )
}
