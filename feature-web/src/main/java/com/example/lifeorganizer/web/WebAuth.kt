package com.example.lifeorganizer.web

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/** A browser that was paired with the pairing code. Only a hash of its token is kept. */
data class PairedBrowser(val id: String, val name: String, val tokenHash: String, val created: Long, val lastUsed: Long) {
    companion object {
        fun encodeAll(list: List<PairedBrowser>): String = JSONArray(list.map {
            JSONObject().put("id", it.id).put("name", it.name).put("hash", it.tokenHash).put("created", it.created).put("lastUsed", it.lastUsed)
        }).toString()

        fun decodeAll(s: String?): List<PairedBrowser> = runCatching {
            val a = JSONArray(s ?: return emptyList())
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                PairedBrowser(o.getString("id"), o.optString("name"), o.getString("hash"), o.optLong("created"), o.optLong("lastUsed"))
            }
        }.getOrDefault(emptyList())
    }
}

interface TokenStore {
    fun load(): List<PairedBrowser>
    fun save(browsers: List<PairedBrowser>)
}

/**
 * Pairing: the phone shows a 6-digit code, the browser sends it once and gets a long random token.
 * After 5 wrong codes a new code is drawn, so guessing does not work.
 */
class WebAuth(private val store: TokenStore, private val clock: () -> Long = System::currentTimeMillis) {
    private val random = SecureRandom()
    private var failures = 0

    @Volatile var code: String = newCode()
        private set

    @Synchronized
    fun newCode(): String {
        code = (0 until 6).joinToString("") { random.nextInt(10).toString() }
        failures = 0
        return code
    }

    /** Returns the token for a correct code, else null. */
    @Synchronized
    fun pair(candidate: String, name: String): String? {
        if (candidate.trim() != code) {
            if (++failures >= 5) newCode()
            return null
        }
        val bytes = ByteArray(32).also(random::nextBytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        val id = ByteArray(8).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        store.save(store.load() + PairedBrowser(id, name.take(80).ifBlank { "Browser" }, hash(token), clock(), clock()))
        // A used code is gone: the next browser needs a fresh one
        newCode()
        return token
    }

    @Synchronized
    fun verify(token: String): Boolean {
        val h = hash(token)
        val list = store.load()
        val match = list.firstOrNull { it.tokenHash == h } ?: return false
        if (clock() - match.lastUsed > 60_000) store.save(list.map { if (it.id == match.id) it.copy(lastUsed = clock()) else it })
        return true
    }

    fun browsers(): List<PairedBrowser> = store.load()

    @Synchronized
    fun revoke(id: String) = store.save(store.load().filterNot { it.id == id })

    private fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
}
