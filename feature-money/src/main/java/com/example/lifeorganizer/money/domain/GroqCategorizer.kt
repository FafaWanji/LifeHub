package com.example.lifeorganizer.money.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sorts unknown payees into categories with Groq. Only payee names and category names leave the
 * device – never amounts, dates or IBANs. Any failure returns an empty map (import continues).
 */
object GroqCategorizer {
    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"

    fun buildRequest(payees: List<String>, categories: List<String>): JSONObject {
        val system = """
            You sort bank transaction counterparties into budget categories.
            Categories: ${categories.joinToString(", ")}
            Answer ONLY with JSON: {"items":[{"payee":"<exactly as given>","category":"<one of the categories>"}]}
            Use the category names exactly as listed. Leave out payees you are unsure about.
        """.trimIndent()
        return JSONObject()
            .put("model", "openai/gpt-oss-120b")
            .put("temperature", 0.0)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", JSONArray(payees).toString()))
            )
    }

    fun parseResponse(body: String, categories: List<String>): Map<String, String> = runCatching {
        val content = JSONObject(body).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
        val items = JSONObject(content.replace("```json", "").replace("```", "").trim()).optJSONArray("items")
            ?: return emptyMap()
        val byLower = categories.associateBy { it.lowercase() }
        (0 until items.length()).mapNotNull { i ->
            val o = items.optJSONObject(i) ?: return@mapNotNull null
            val category = byLower[o.optString("category").lowercase()] ?: return@mapNotNull null
            o.optString("payee").takeIf { it.isNotBlank() }?.let { it to category }
        }.toMap()
    }.getOrDefault(emptyMap())

    suspend fun categorize(apiKey: String, payees: List<String>, categories: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || payees.isEmpty() || categories.isEmpty()) return@withContext emptyMap()
            payees.chunked(100).flatMap { chunk ->
                runCatching {
                    val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 30_000
                    connection.doOutput = true
                    OutputStreamWriter(connection.outputStream).use { it.write(buildRequest(chunk, categories).toString()) }
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching emptyMap()
                    parseResponse(connection.inputStream.bufferedReader().use { it.readText() }, categories)
                }.getOrDefault(emptyMap()).toList()
            }.toMap()
        }
}
