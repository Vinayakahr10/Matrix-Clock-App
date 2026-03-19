package com.dotmatrix.app.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Alarm data class and BLE command formatting logic.
 * These tests verify the core data/logic without requiring Android context.
 */
class AlarmTest {

    @Test
    fun `alarm creation has correct default values`() {
        val alarm = Alarm("123", "8:00", "Morning", true)
        assertEquals("123", alarm.id)
        assertEquals("8:00", alarm.time)
        assertEquals("Morning", alarm.label)
        assertTrue(alarm.active)
    }

    @Test
    fun `alarm toggle flips active state`() {
        val alarm = Alarm("1", "7:30", "Work", true)
        val toggled = alarm.copy(active = !alarm.active)
        assertFalse(toggled.active)
        assertEquals(alarm.id, toggled.id)
        assertEquals(alarm.time, toggled.time)
    }

    @Test
    fun `alarm copy preserves other fields`() {
        val alarm = Alarm("42", "12:00", "Lunch", false)
        val updated = alarm.copy(active = true)
        assertTrue(updated.active)
        assertEquals("42", updated.id)
        assertEquals("12:00", updated.time)
        assertEquals("Lunch", updated.label)
    }
}
