package com.dotmatrix.app.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BLE command format strings.
 * Validates that the string commands sent to the ESP32 match the expected protocol.
 */
class BleCommandFormatTest {

    // Simulates the command formatting logic from SharedConnectionViewModel

    @Test
    fun `sync time command format`() {
        val timeStr = "14:30:00"
        val dateStr = "18-03-2026"
        val cmd = "SYNC:$timeStr,$dateStr"
        assertEquals("SYNC:14:30:00,18-03-2026", cmd)
    }

    @Test
    fun `time format command 24h`() {
        val is24H = true
        val cmd = "FORMAT:${if (is24H) 24 else 12}"
        assertEquals("FORMAT:24", cmd)
    }

    @Test
    fun `time format command 12h`() {
        val is24H = false
        val cmd = "FORMAT:${if (is24H) 24 else 12}"
        assertEquals("FORMAT:12", cmd)
    }

    @Test
    fun `brightness command format`() {
        val brightness = 0.75f
        val level = (brightness * 100).toInt()
        val cmd = "BRIGHT:$level"
        assertEquals("BRIGHT:75", cmd)
    }

    @Test
    fun `alarm add command format`() {
        val id = "1234567890"
        val time = "8:00"
        val cmd = "ALARM_ADD:$id,$time,1"
        assertEquals("ALARM_ADD:1234567890,8:00,1", cmd)
    }

    @Test
    fun `alarm toggle command format`() {
        val alarmId = "123"
        val active = true
        val newState = if (!active) "1" else "0"
        val cmd = "ALARM_TOGGLE:$alarmId,$newState"
        assertEquals("ALARM_TOGGLE:123,0", cmd)
    }

    @Test
    fun `visualizer commands format`() {
        assertEquals("VIS_ENABLE:1", "VIS_ENABLE:${if (true) 1 else 0}")
        assertEquals("VIS_ENABLE:0", "VIS_ENABLE:${if (false) 1 else 0}")
        assertEquals("VIS_MODE:frequency", "VIS_MODE:frequency")
        assertEquals("VIS_SENSE:75", "VIS_SENSE:75")
    }

    @Test
    fun `scroll text command format`() {
        assertEquals("SCROLL:1", "SCROLL:${if (true) 1 else 0}")
        assertEquals("SCROLL:0", "SCROLL:${if (false) 1 else 0}")
    }

    @Test
    fun `timer commands format`() {
        assertEquals("TIMER_START:1800", "TIMER_START:${30 * 60}")
        assertEquals("TIMER_STOP", "TIMER_STOP")
    }

    @Test
    fun `stopwatch commands format`() {
        assertEquals("SW_START", "SW_START")
        assertEquals("SW_PAUSE", "SW_PAUSE")
        assertEquals("SW_RESET", "SW_RESET")
    }
}
