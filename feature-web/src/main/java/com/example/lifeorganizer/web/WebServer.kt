package com.example.lifeorganizer.web

import android.content.Context
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.json.JSONObject

/** SharedPreferences-backed store of paired browsers. */
class PrefsTokenStore(context: Context) : TokenStore {
    private val prefs = context.applicationContext.getSharedPreferences("web_access", Context.MODE_PRIVATE)
    override fun load() = PairedBrowser.decodeAll(prefs.getString("browsers", null))
    override fun save(browsers: List<PairedBrowser>) { prefs.edit().putString("browsers", PairedBrowser.encodeAll(browsers)).apply() }
}

/**
 * Local web server: serves the browser page from the app's assets and the JSON API under /api.
 * Every /api call except info and pairing needs a paired browser's token.
 */
class WebServer(context: Context, val port: Int = DEFAULT_PORT) {
    private val app = context.applicationContext
    val auth = WebAuth(PrefsTokenStore(app))
    private val api = WebApi(app)
    private var engine: EmbeddedServer<*, *>? = null

    /** Time of the last request; the service stops the server after a while without any. */
    @Volatile var lastActivity = System.currentTimeMillis()
        private set

    fun start() {
        engine = embeddedServer(CIO, port = port, host = "0.0.0.0") { module() }.start(wait = false)
    }

    fun stop() {
        engine?.stop(200, 1000)
        engine = null
        api.close()
    }

    private fun Application.module() {
        routing {
            route("{...}") {
                handle { dispatch(call) }
            }
        }
    }

    private suspend fun dispatch(call: ApplicationCall) {
        lastActivity = System.currentTimeMillis()
        call.response.header("X-Content-Type-Options", "nosniff")
        call.response.header("Referrer-Policy", "no-referrer")
        call.response.header("Cache-Control", "no-store")
        val path = call.request.path()
        when {
            path == "/" || path == "/index.html" -> asset(call, "index.html", ContentType.Text.Html)
            path == "/app.js" -> asset(call, "app.js", ContentType.Text.JavaScript)
            path == "/app.css" -> asset(call, "app.css", ContentType.Text.CSS)
            path.startsWith("/api/") -> {
                val method = call.request.httpMethod.value
                val body = if (method == "POST" || method == "PUT") call.receiveText().take(MAX_BODY) else ""
                val response = when {
                    path == "/api/info" -> api.info()
                    path == "/api/pair" && method == "POST" -> pair(body)
                    !authorized(call) -> ApiResponse.error(401, "unauthorized")
                    else -> api.handle(method, path, call.request.queryParameters.entries().associate { it.key to it.value.first() }, body)
                }
                call.respondText(response.body, ContentType.Application.Json, HttpStatusCode.fromValue(response.status))
            }
            else -> call.respondText("Not found", ContentType.Text.Plain, HttpStatusCode.NotFound)
        }
    }

    private fun authorized(call: ApplicationCall): Boolean {
        val header = call.request.headers["Authorization"] ?: return false
        return header.startsWith("Bearer ") && auth.verify(header.removePrefix("Bearer ").trim())
    }

    private fun pair(body: String): ApiResponse {
        val j = runCatching { JSONObject(body) }.getOrNull() ?: return ApiResponse.error(400, "bad_request")
        val token = auth.pair(j.optString("code"), j.optString("name")) ?: return ApiResponse.error(403, "wrong_code")
        return ApiResponse.ok(JSONObject().put("token", token))
    }

    private suspend fun asset(call: ApplicationCall, name: String, type: ContentType) {
        if (type == ContentType.Text.Html) {
            call.response.header("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; base-uri 'none'")
            call.response.header("X-Frame-Options", "DENY")
        }
        call.respondBytes(app.assets.open("web/$name").use { it.readBytes() }, type)
    }

    companion object {
        const val DEFAULT_PORT = 8080
        private const val MAX_BODY = 500_000
    }
}
