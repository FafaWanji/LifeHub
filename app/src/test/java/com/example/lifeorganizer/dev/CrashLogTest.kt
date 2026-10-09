package com.example.lifeorganizer.dev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogTest {
    @Test
    fun `summary names the innermost cause and the LifeHub line`() {
        val inner = IllegalStateException("note 42 missing")
        val lines = CrashLog.summary(RuntimeException("wrapper", inner)).lines()
        assertEquals("IllegalStateException: note 42 missing", lines[0])
        assertTrue(lines[1], lines[1].startsWith("@ CrashLogTest.kt:"))
    }

    @Test
    fun `summary without message or own frame is just the type`() {
        val e = NullPointerException()
        e.stackTrace = arrayOf(StackTraceElement("java.util.Foo", "bar", "Foo.java", 1))
        assertEquals("NullPointerException", CrashLog.summary(e))
    }
}
