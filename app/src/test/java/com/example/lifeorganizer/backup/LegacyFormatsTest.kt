package com.example.lifeorganizer.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyFormatsTest {

    private val waypointsIrl = """
        {"waypoints":[{"id":7,"name":"Max","address":"Hauptstraße 1, Berlin","notes":"2. OG","lat":52.5,"lng":13.4,
                       "isPinned":true,"lastUsed":1,"lastEdited":2},
                      {"id":8,"name":"Oma","address":"Gartenweg 3","notes":"","isPinned":false,"lastUsed":0,"lastEdited":0}],
         "labels":[{"id":3,"name":"Familie","colorHex":"#F48FB1"}],
         "crossRefs":[{"waypointId":8,"labelId":3}]}
    """.trimIndent()

    private val lifeBase = """
        {"notes":[{"id":"a","title":"Ideen","content":"- Buch lesen","labelId":"2","date":"2025-03-01T10:00:00.000Z","isPinned":true,"updatedAt":1740823200000},
                  {"id":"b","title":"","content":"","labelId":"1","date":"2025-03-02"}],
         "labels":[{"id":"1","name":"Allgemein","color":"bg-yellow-200","textColor":"text-yellow-900"},
                   {"id":"2","name":"Arbeit","color":"bg-blue-200","textColor":"text-blue-900"}],
         "updatedAt":1740823200000}
    """.trimIndent()

    @Test
    fun `formats are detected`() {
        assertEquals(ImportSource.ICS_CALENDAR, LegacyFormats.detect("BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR"))
        assertEquals(ImportSource.WAYPOINTS_IRL, LegacyFormats.detect(waypointsIrl))
        assertEquals(ImportSource.LIFEBASE, LegacyFormats.detect(lifeBase))
        assertEquals(ImportSource.CHECKLIST, LegacyFormats.detect(realChecklist()))
        assertEquals(ImportSource.LIFEORGANIZER_BACKUP, LegacyFormats.detect("""{"format":"lifeorganizer-backup","version":1}"""))
        assertEquals(ImportSource.UNKNOWN, LegacyFormats.detect("""{"foo":1}"""))
        assertEquals(ImportSource.UNKNOWN, LegacyFormats.detect("kein json"))
    }

    @Test
    fun `waypoints keep labels by name`() {
        val (labels, waypoints) = LegacyFormats.parseWaypointsIrl(waypointsIrl)
        assertEquals(listOf("Familie"), labels.map { it.name })
        val max = waypoints.first { it.name == "Max" }
        assertEquals("2. OG", max.notes)
        assertEquals(52.5, max.lat!!, 0.0)
        assertTrue(max.isPinned)
        assertEquals(listOf("Familie"), waypoints.first { it.name == "Oma" }.labelNames)
    }

    @Test
    fun `lifebase notes get label and sticky colour, empty notes are dropped`() {
        val (labels, notes) = LegacyFormats.parseLifeBase(lifeBase)
        assertEquals(2, labels.size)
        assertEquals(1, notes.size)
        val note = notes.single()
        assertEquals(listOf("Arbeit"), note.labelNames)
        assertEquals(0xFF90CAF9.toInt(), note.colorLabel)
        assertTrue(note.isPinned)
        assertEquals(1740823200000L, note.createdAt)
    }

    @Test
    fun `real checklist file becomes checklist notes`() {
        val notes = LegacyFormats.parseChecklist(realChecklist())
        // Page "2" is empty and skipped
        assertEquals(listOf("Allgemein"), notes.map { it.title })
        val content = notes.single().content
        assertTrue(content, content.startsWith("- [ ] Snitch (1/4)"))
        assertTrue(content, content.contains("- [ ] Bombardier (4/5)"))
        assertTrue(content, content.contains("- [ ] Rusted Gear (1/3)"))
        // The free-text notes of the page are kept below the list
        assertTrue(content, content.trimEnd().endsWith("rfwerewewrwe"))
    }

    @Test
    fun `finished counters are ticked and named groups nest`() {
        val json = """{"Einkauf":{"tasks":[
            {"text":"Wasser","type":"counter","current":6,"target":6,"done":false},
            {"text":"Party","type":"group","done":false,"subtasks":[{"text":"Chips","done":true}]}],"notes":""}}"""
        assertEquals("- [x] Wasser (6/6)\n- [ ] Party\n  - [x] Chips", LegacyFormats.parseChecklist(json).single().content)
    }

    @Test
    fun `tailwind and hex colours are mapped`() {
        assertEquals(0xFFA5D6A7.toInt(), LegacyFormats.tailwindToColor("bg-green-200"))
        assertEquals(0xFF112233.toInt(), LegacyFormats.tailwindToColor("#112233"))
    }

    private fun realChecklist(): String =
        javaClass.classLoader!!.getResource("checklist_data.json")!!.readText()
}
