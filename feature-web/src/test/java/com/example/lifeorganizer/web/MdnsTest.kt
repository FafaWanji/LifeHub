package com.example.lifeorganizer.web

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class MdnsTest {
    /** Builds a query like Windows/macOS send it: one question, optional QU bit. */
    private fun query(name: String, type: Int, id: Int = 0, unicast: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf((id shr 8).toByte(), id.toByte(), 0, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        name.split('.').forEach { label -> out.write(label.length); out.write(label.toByteArray()) }
        out.write(0)
        out.write(byteArrayOf(0, type.toByte(), (if (unicast) 0x80 else 0).toByte(), 1))
        return out.toByteArray()
    }

    @Test fun parsesQuestion() {
        val q = Mdns.parseQuestions(query("LifeHub.local", Mdns.TYPE_A, unicast = true)).single()
        assertEquals("lifehub.local", q.name)
        assertEquals(Mdns.TYPE_A, q.type)
        assertTrue(q.unicastResponse)
    }

    @Test fun answersOnlyOwnNameForAOrAny() {
        assertTrue(Mdns.wants(Mdns.parseQuestions(query("lifehub.local", Mdns.TYPE_A)), "lifehub"))
        assertTrue(Mdns.wants(Mdns.parseQuestions(query("LIFEHUB.local", 255)), "lifehub"))
        assertEquals(false, Mdns.wants(Mdns.parseQuestions(query("lifehub.local", 28)), "lifehub"))
        assertEquals(false, Mdns.wants(Mdns.parseQuestions(query("other.local", Mdns.TYPE_A)), "lifehub"))
    }

    @Test fun compressedNamesAreFollowed() {
        // Second question points back to the first name (offset 12)
        val first = query("lifehub.local", Mdns.TYPE_A)
        val packet = first.copyOf(first.size + 6)
        packet[5] = 2 // qdcount = 2
        val extra = byteArrayOf(0xC0.toByte(), 12, 0, 1, 0, 1)
        extra.copyInto(packet, first.size)
        val questions = Mdns.parseQuestions(packet)
        assertEquals(listOf("lifehub.local", "lifehub.local"), questions.map { it.name })
    }

    @Test fun answerCarriesAddress() {
        val answer = Mdns.buildAnswer(id = 0, host = "lifehub", ip = byteArrayOf(192.toByte(), 168.toByte(), 1, 20), question = null)
        // Header: response + authoritative, one answer
        assertEquals(0x84, answer[2].toInt() and 0xFF)
        assertEquals(1, answer[7].toInt())
        assertArrayEquals(byteArrayOf(192.toByte(), 168.toByte(), 1, 20), answer.copyOfRange(answer.size - 4, answer.size))
        // The answer name parses back
        assertTrue(answer.copyOfRange(12, 12 + 15).contentEquals(byteArrayOf(7) + "lifehub".toByteArray() + byteArrayOf(5) + "local".toByteArray() + byteArrayOf(0)))
    }

    @Test fun legacyUnicastReplyEchoesIdAndQuestion() {
        val answer = Mdns.buildAnswer(id = 0x1234, host = "lifehub", ip = byteArrayOf(10, 0, 0, 5), question = Mdns.Question("lifehub.local", Mdns.TYPE_A, false))
        assertEquals(0x12, answer[0].toInt())
        assertEquals(0x34, answer[1].toInt())
        assertEquals(1, answer[5].toInt()) // qdcount
    }

    @Test fun garbageIsIgnored() {
        assertEquals(emptyList<Mdns.Question>(), Mdns.parseQuestions(byteArrayOf(1, 2, 3)))
        val loop = byteArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0xC0.toByte(), 12, 0, 1, 0, 1)
        assertEquals(emptyList<Mdns.Question>(), Mdns.parseQuestions(loop))
    }

    @Test fun namesAreSanitized() {
        assertEquals("fahrat-pc", WebSettings.sanitizeName(" Fahrat PC! "))
        assertEquals("lifehub", WebSettings.sanitizeName("***"))
        assertEquals("a".repeat(30), WebSettings.sanitizeName("a".repeat(50)))
        assertEquals("muenchen", WebSettings.sanitizeName("München"))
    }

    @Test fun portsAreChecked() {
        assertEquals(true, WebSettings.validPort(8080))
        assertEquals(false, WebSettings.validPort(80))
        assertEquals(false, WebSettings.validPort(70000))
    }
}
