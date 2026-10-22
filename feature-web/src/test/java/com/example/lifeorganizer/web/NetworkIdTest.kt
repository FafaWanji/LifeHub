package com.example.lifeorganizer.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkIdTest {
    private val home = mapOf("FritzBox" to setOf("ssid:FritzBox", "gw:192.168.178.1/24"))

    @Test
    fun rememberedWifiIsRecognisedByNameOrRouter() {
        assertTrue(NetworkId.matches(setOf("ssid:FritzBox", "gw:192.168.178.1/24"), home))
        // Name not readable (no location access): the router still identifies it
        assertTrue(NetworkId.matches(setOf("gw:192.168.178.1/24"), home))
    }

    @Test
    fun otherOrNoWifiDoesNotMatch() {
        assertFalse(NetworkId.matches(setOf("ssid:eduroam", "gw:10.0.0.1/16"), home))
        assertFalse(NetworkId.matches(emptySet(), home))
        assertFalse(NetworkId.matches(setOf("ssid:FritzBox"), emptyMap()))
    }

    @Test
    fun onlyWifiAndAlwaysKeepRunning() {
        assertFalse(AutoStart.OFF.persistent)
        assertFalse(AutoStart.APP_OPEN.persistent)
        assertTrue(AutoStart.WIFI.persistent)
        assertTrue(AutoStart.ALWAYS.persistent)
    }
}
