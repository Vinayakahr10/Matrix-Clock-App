package com.dotmatrix.app.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OTA state machine transitions.
 */
class OTAStateTest {

    @Test
    fun `all OTA states exist`() {
        val states = OTAState.values()
        assertEquals(9, states.size)
        assertTrue(states.contains(OTAState.Idle))
        assertTrue(states.contains(OTAState.Checking))
        assertTrue(states.contains(OTAState.UpdateAvailable))
        assertTrue(states.contains(OTAState.NoUpdate))
        assertTrue(states.contains(OTAState.Downloading))
        assertTrue(states.contains(OTAState.ReadyToInstall))
        assertTrue(states.contains(OTAState.Installing))
        assertTrue(states.contains(OTAState.Success))
        assertTrue(states.contains(OTAState.Error))
    }

    @Test
    fun `OTA state enum ordinals are consistent`() {
        assertEquals(0, OTAState.Idle.ordinal)
        assertEquals(1, OTAState.Checking.ordinal)
        assertEquals(8, OTAState.Error.ordinal)
    }
}
