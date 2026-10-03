package com.example.lifeorganizer.core.smartadd

import org.junit.Assert.assertEquals
import org.junit.Test

class WaypointResolutionTest {

    private val waypoints = listOf(
        "Max" to "Hauptstraße 1, 10115 Berlin",
        "Max Arbeit" to "Industrieweg 5, 10117 Berlin",
        "Oma" to "Gartenweg 3, 14467 Potsdam"
    )

    @Test
    fun `waypoint name in the text resolves to its saved address`() {
        val address = SmartAddEngine.resolveWaypointAddress("Sonntag Treffen bei Max", "Treffen", "", waypoints)
        assertEquals("Hauptstraße 1, 10115 Berlin", address)
    }

    @Test
    fun `model returning only the name is replaced by the address`() {
        val address = SmartAddEngine.resolveWaypointAddress("Kaffee bei Oma", "Kaffee", "Oma", waypoints)
        assertEquals("Gartenweg 3, 14467 Potsdam", address)
    }

    @Test
    fun `longest matching name wins`() {
        val address = SmartAddEngine.resolveWaypointAddress("Montag Meeting bei Max Arbeit", "Meeting", "", waypoints)
        assertEquals("Industrieweg 5, 10117 Berlin", address)
    }

    @Test
    fun `names only match as whole words`() {
        val address = SmartAddEngine.resolveWaypointAddress("Maximilian kommt vorbei", "Besuch", "", waypoints)
        assertEquals("", address)
    }

    @Test
    fun `an address already resolved by the model is kept`() {
        val address = SmartAddEngine.resolveWaypointAddress("bei Max", "x", "Hauptstraße 1, 10115 Berlin", waypoints)
        assertEquals("Hauptstraße 1, 10115 Berlin", address)
    }

    @Test
    fun `unrelated text keeps the model location`() {
        val address = SmartAddEngine.resolveWaypointAddress("Zahnarzt Dienstag", "Zahnarzt", "Dr. Müller, Berlin", waypoints)
        assertEquals("Dr. Müller, Berlin", address)
    }
}
