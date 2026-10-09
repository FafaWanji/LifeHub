package com.example.lifeorganizer.web

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.money.data.MoneyDatabase
import com.example.lifeorganizer.notes.data.NotesDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId

/** Starts the real server on the device and talks HTTP to it, like the browser page does. */
@RunWith(AndroidJUnit4::class)
class WebServerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var server: WebServer

    @Before fun setUp() {
        AppDatabase.getDatabase(context).clearAllTables()
        NotesDatabase.getDatabase(context).clearAllTables()
        MoneyDatabase.getDatabase(context).clearAllTables()
        server = WebServer(context, port = 18080)
        server.start()
    }

    @After fun tearDown() = server.stop()

    private fun http(method: String, path: String, body: String? = null, token: String? = null): Pair<Int, String> {
        val c = URL("http://127.0.0.1:18080$path").openConnection() as HttpURLConnection
        c.requestMethod = method
        token?.let { c.setRequestProperty("Authorization", "Bearer $it") }
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        return code to text
    }

    private fun pair(): String {
        val (code, body) = http("POST", "/api/pair", JSONObject().put("code", server.auth.code).put("name", "Test").toString())
        assertEquals(200, code)
        return JSONObject(body).getString("token")
    }

    private fun millis(y: Int, m: Int, d: Int, h: Int) = LocalDateTime.of(y, m, d, h, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun pageIsServedWithoutToken() {
        val (code, body) = http("GET", "/")
        assertEquals(200, code)
        assertTrue(body.contains("<html"))
    }

    @Test fun tabIconIsServed() {
        val (code, body) = http("GET", "/favicon.svg")
        assertEquals(200, code)
        assertTrue(body.contains("<svg"))
        assertTrue(http("GET", "/").second.contains("favicon.svg"))
    }

    @Test fun apiNeedsToken() {
        assertEquals(401, http("GET", "/api/notes").first)
        assertEquals(401, http("GET", "/api/notes", token = "wrong").first)
    }

    @Test fun wrongCodeIsRejected() {
        val wrong = if (server.auth.code == "000000") "111111" else "000000"
        assertEquals(403, http("POST", "/api/pair", JSONObject().put("code", wrong).toString()).first)
    }

    @Test fun notesRoundTripAndChangeCounter() {
        val token = pair()
        val before = JSONObject(http("GET", "/api/changes", token = token).second).getLong("version")
        val (code, created) = http("POST", "/api/notes", JSONObject().put("title", "Vom PC").put("content", "- [ ] Milch").toString(), token)
        assertEquals(200, code)
        val id = JSONObject(created).getLong("id")
        assertEquals("Vom PC", JSONArray(http("GET", "/api/notes", token = token).second).getJSONObject(0).getString("title"))

        var after = before
        repeat(20) { if (after == before) { Thread.sleep(100); after = JSONObject(http("GET", "/api/changes", token = token).second).getLong("version") } }
        assertNotEquals(before, after)

        http("PUT", "/api/notes/$id", JSONObject().put("title", "Geändert").put("content", "- [x] Milch").toString(), token)
        assertEquals("- [x] Milch", JSONArray(http("GET", "/api/notes", token = token).second).getJSONObject(0).getString("content"))
        http("DELETE", "/api/notes/$id", token = token)
        assertEquals(0, JSONArray(http("GET", "/api/notes", token = token).second).length())
    }

    @Test fun calendarSeriesRoundTrip() {
        val token = pair()
        val body = JSONObject().put("title", "Sport").put("start", millis(2026, 10, 20, 18)).put("end", millis(2026, 10, 20, 19))
            .put("allDay", false).put("recurrence", "FREQ=WEEKLY").put("reminders", JSONArray().put(30))
        val id = JSONObject(http("POST", "/api/calendar/events", body.toString(), token).second).getLong("id")
        val month = JSONArray(http("GET", "/api/calendar/events?from=2026-10-01&to=2026-10-31", token = token).second)
        assertEquals(2, month.length())
        assertEquals(listOf(30), (0 until 1).map { month.getJSONObject(0).getJSONArray("reminders").getInt(it) })

        http("PUT", "/api/calendar/events/$id", body.put("title", "Laufen").toString(), token)
        val renamed = JSONArray(http("GET", "/api/calendar/events?from=2026-10-01&to=2026-10-31", token = token).second)
        assertEquals("Laufen", renamed.getJSONObject(1).getString("title"))

        http("DELETE", "/api/calendar/events/$id", token = token)
        assertEquals(0, JSONArray(http("GET", "/api/calendar/events?from=2026-10-01&to=2026-10-31", token = token).second).length())
    }

    @Test fun moneyMonth() {
        val token = pair()
        val day = java.time.LocalDate.of(2026, 10, 9).toEpochDay()
        assertEquals(200, http("POST", "/api/money/transactions", JSONObject().put("title", "Döner").put("amount", -1250).put("epochDay", day).toString(), token).first)
        val month = JSONObject(http("GET", "/api/money/month?month=2026-10", token = token).second)
        assertEquals(-1250L, month.getLong("expense"))
        assertEquals(1, month.getJSONArray("transactions").length())
        assertTrue(JSONArray(http("GET", "/api/money/categories", token = token).second).length() >= 10)
    }
}
