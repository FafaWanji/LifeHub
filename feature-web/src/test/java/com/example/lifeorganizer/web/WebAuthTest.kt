package com.example.lifeorganizer.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAuthTest {
    private class MemoryStore : TokenStore {
        var saved: List<PairedBrowser> = emptyList()
        override fun load() = saved
        override fun save(browsers: List<PairedBrowser>) { saved = browsers }
    }

    private var now = 1_000L
    private val store = MemoryStore()
    private val auth = WebAuth(store) { now }

    @Test fun pairWithCodeGivesWorkingToken() {
        val token = auth.pair(auth.code, "Firefox")
        assertNotNull(token)
        assertTrue(auth.verify(token!!))
        assertEquals("Firefox", store.saved.single().name)
    }

    @Test fun tokenIsStoredOnlyAsHash() {
        val token = auth.pair(auth.code, "Chrome")!!
        assertFalse(store.saved.single().tokenHash.contains(token))
    }

    @Test fun wrongCodeIsRejected() {
        assertNull(auth.pair("000000".takeIf { it != auth.code } ?: "111111", "x"))
        assertFalse(auth.verify("nonsense"))
    }

    @Test fun fiveWrongCodesReplaceTheCode() {
        val first = auth.code
        repeat(5) { auth.pair("wrong", "x") }
        assertNotEquals(first, auth.code)
        assertNull(auth.pair(first, "x"))
    }

    @Test fun codeIsSixDigits() = assertTrue(Regex("\\d{6}").matches(auth.code))

    @Test fun revokeRemovesAccess() {
        val token = auth.pair(auth.code, "Edge")!!
        auth.revoke(store.saved.single().id)
        assertFalse(auth.verify(token))
    }

    @Test fun verifyUpdatesLastUsed() {
        val token = auth.pair(auth.code, "Edge")!!
        // "last used" is written at most once a minute
        now = 30_000L
        auth.verify(token)
        assertEquals(1_000L, store.saved.single().lastUsed)
        now = 120_000L
        auth.verify(token)
        assertEquals(120_000L, store.saved.single().lastUsed)
    }

    @Test fun serializationRoundTrip() {
        val b = PairedBrowser("id1", "Firefox; \"quoted\"", "abc", 1, 2)
        assertEquals(listOf(b), PairedBrowser.decodeAll(PairedBrowser.encodeAll(listOf(b))))
    }
}
