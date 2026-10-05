package com.example.lifeorganizer.core.smartadd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SmartAddEngine {

    data class ContextData(
        val apiKey: String,
        val categories: List<Pair<Long, String>> = emptyList(), // ID to Name
        val waypoints: List<Pair<String, String>> = emptyList() // Name to Address
    )

    suspend fun process(inputText: String, contextData: ContextData): List<SmartResult>? = withContext(Dispatchers.IO) {
        // Without an API key, Smart Add works offline with simple rules.
        if (contextData.apiKey.isBlank()) return@withContext OfflineParser.parse(inputText, contextData.waypoints).ifEmpty { null }
        try {
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val currentTimeStr = now.format(formatter)
            val timeZone = ZoneId.systemDefault().id
            
            val categoriesStr = contextData.categories.joinToString(", ") { "${it.first}: ${it.second}" }
            val waypointsStr = contextData.waypoints.joinToString(", ") { "'${it.first}' -> ${it.second}" }

            val systemPrompt = """
                You are the intelligence engine of 'LifeHub', an app that handles both Calendar Events and Notes.
                Analyze the user's text and extract the data into a JSON array of items. 
                Determine for each item whether it's an 'event' or a 'note'.
                
                - EVENT: Has a specific time, date, or relative timeframe (e.g., "tomorrow at 8pm", "dentist appointment", "party on sunday").
                - NOTE: Just capturing information, ideas, lists, or thoughts without a specific timeframe.
                
                Use the context to resolve relative dates and addresses:
                Current Date and Time: $currentTimeStr
                Timezone: $timeZone
                Available Categories (ID: Name): $categoriesStr
                Saved Waypoints (Name -> Address): $waypointsStr
                
                JSON Format required:
                {
                  "items": [
                    {
                      "type": "event",
                      "title": "Short title",
                      "description": "Additional details",
                      "location": "Resolved physical address",
                      "startTimeIso": "2026-07-10T20:00:00",
                      "endTimeIso": "2026-07-10T21:00:00",
                      "remindersInMinutes": [60],
                      "categoryId": 1,
                      "isBirthday": false,
                      "birthYear": null
                    },
                    {
                      "type": "note",
                      "title": "Short title",
                      "content": "Full content of the note",
                      "isChecklist": false
                    }
                  ]
                }
                
                Event Rules:
                - If the text mentions a saved Waypoint name (e.g. "at max monopoly"), use its exact address from the context for the location.
                - startTimeIso and endTimeIso must be formatted exactly as yyyy-MM-ddTHH:mm:ss.
                - If no end time is specified, default to 1 hour after the start time.
                - The title should be concise. For birthdays, just use the person's name as the title.
                
                Note Rules:
                - If the text looks like a list of tasks or shopping items, set isChecklist to true and format content with line breaks.
                
                General Rules:
                - Ignore chat artifacts from screenshots (e.g., timestamps like "18:56").
                - Return ONLY JSON. No markdown formatting, no explanatory text.
            """.trimIndent()

            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer ${contextData.apiKey.trim()}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("model", "openai/gpt-oss-120b")
                put("temperature", 0.0)
                put("response_format", JSONObject().put("type", "json_object"))
                
                val messages = JSONArray()
                messages.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", inputText)
                })
                put("messages", messages)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream.bufferedReader().use { it.readText() }
                throw Exception("API Error ($responseCode): $errorStream")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseBody)
            
            val contentString = responseJson.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                
            val jsonString = contentString.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(jsonString)

            val itemsArray = json.optJSONArray("items") ?: return@withContext null
            val results = mutableListOf<SmartResult>()

            for (i in 0 until itemsArray.length()) {
                val itemJson = itemsArray.getJSONObject(i)
                val type = itemJson.optString("type", "note")

                if (type == "event") {
                    val startIso = itemJson.optString("startTimeIso", "")
                    val endIso = itemJson.optString("endTimeIso", "")
                    
                    val parsedStartMillis = try {
                        LocalDateTime.parse(startIso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (e: Exception) { System.currentTimeMillis() }
                    
                    val parsedEndMillis = try {
                        LocalDateTime.parse(endIso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (e: Exception) { parsedStartMillis + 3600000 }

                    val remindersArray = itemJson.optJSONArray("remindersInMinutes")
                    val parsedReminders = mutableListOf<Int>()
                    if (remindersArray != null) {
                        for (j in 0 until remindersArray.length()) {
                            parsedReminders.add(remindersArray.optInt(j, 60))
                        }
                    }

                    val title = itemJson.optString("title", "New Event")
                    val modelLocation = com.example.lifeorganizer.core.util.Places.clean(itemJson.optString("location", "")).orEmpty()
                    results.add(
                        SmartResult.Event(
                            title = title,
                            description = itemJson.optString("description", ""),
                            location = resolveWaypointAddress(inputText, title, modelLocation, contextData.waypoints),
                            startTimeMillis = parsedStartMillis,
                            endTimeMillis = parsedEndMillis,
                            reminderMinutesBefore = parsedReminders,
                            categoryId = if (itemJson.has("categoryId") && !itemJson.isNull("categoryId")) itemJson.optLong("categoryId") else null,
                            isBirthday = itemJson.optBoolean("isBirthday", false),
                            birthYear = if (itemJson.has("birthYear") && !itemJson.isNull("birthYear")) itemJson.optInt("birthYear") else null
                        )
                    )
                } else {
                    results.add(
                        SmartResult.Note(
                            title = itemJson.optString("title", "New Note"),
                            content = itemJson.optString("content", ""),
                            isChecklist = itemJson.optBoolean("isChecklist", false)
                        )
                    )
                }
            }
            results.ifEmpty { null }
        } catch (e: java.io.IOException) {
            // No network: fall back to the offline rules instead of failing.
            OfflineParser.parse(inputText, contextData.waypoints).ifEmpty { null }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Makes the waypoint link deterministic: if the user's text mentions a saved waypoint
     * (e.g. "Sonntag Treffen bei Max") its stored address wins, even when the model returned
     * only the name or nothing at all. The longest matching name is preferred ("Max Arbeit" over "Max").
     */
    internal fun resolveWaypointAddress(
        inputText: String,
        title: String,
        modelLocation: String,
        waypoints: List<Pair<String, String>>
    ): String {
        if (waypoints.isEmpty()) return modelLocation
        if (waypoints.any { it.second.equals(modelLocation, ignoreCase = true) }) return modelLocation

        val haystack = "$inputText $title $modelLocation"
        val match = waypoints
            .filter { (name, address) -> name.isNotBlank() && address.isNotBlank() }
            .filter { (name, _) ->
                Regex("(?<![\\p{L}\\p{N}])${Regex.escape(name.trim())}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
                    .containsMatchIn(haystack)
            }
            .maxByOrNull { it.first.length }
        return match?.second ?: modelLocation
    }
}
