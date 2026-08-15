package com.soundscheduler.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineExecutionContractTest {
    @Test
    fun `connected charging routine is accepted and retains its transition`() {
        val routine = Routine(
            id = 7,
            title = "Mute while charging",
            type = Routine.TYPE_CHARGING,
            chargingTransition = Routine.CHARGING_TRANSITION_CONNECTED,
            soundProfile = Routine.PROFILE_SILENT
        )

        assertEquals(Routine.CHARGING_TRANSITION_CONNECTED, routine.chargingTransition)
        assertEquals(Routine.PROFILE_SILENT, routine.targetSoundMode())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `charging routine rejects unsupported transition`() {
        Routine(
            title = "Invalid charging routine",
            type = Routine.TYPE_CHARGING,
            chargingTransition = "docked",
            soundProfile = Routine.PROFILE_RING
        )
    }

    @Test
    fun `charging execution accepts both power trigger types`() {
        val connectedExecution = chargingExecution(RoutineExecution.TRIGGER_CHARGING_CONNECTED)
        val disconnectedExecution = chargingExecution(RoutineExecution.TRIGGER_CHARGING_DISCONNECTED)

        assertEquals(RoutineExecution.OUTCOME_APPLIED, connectedExecution.outcomeCode)
        assertEquals(RoutineExecution.OUTCOME_APPLIED, disconnectedExecution.outcomeCode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `execution rejects unknown outcome taxonomy`() {
        RoutineExecution(
            routineId = 7,
            triggerType = RoutineExecution.TRIGGER_CHARGING_CONNECTED,
            requestedMode = Routine.PROFILE_SILENT,
            occurredAtMillis = 1_700_000_000_000L,
            outcomeCode = "unknown_outcome"
        )
    }

    private fun chargingExecution(triggerType: String) = RoutineExecution(
        routineId = 7,
        triggerType = triggerType,
        requestedMode = Routine.PROFILE_VIBRATE,
        occurredAtMillis = 1_700_000_000_000L,
        outcomeCode = RoutineExecution.OUTCOME_APPLIED,
        observedMode = Routine.PROFILE_VIBRATE
    )
}
