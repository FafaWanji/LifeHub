package com.example.lifeorganizer.releases.api

import com.example.lifeorganizer.releases.data.Source
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** A search result to put on the radar. */
data class SearchHit(
    val source: Source, val id: Long, val title: String, val format: String, val status: String,
    /** "2017-10-03" or "2017"; "" = unknown. */
    val premiered: String = "", val imageUrl: String? = null
) {
    val year: Int? get() = premiered.take(4).toIntOrNull()
}

/** One announced release: "ep6", "E6", time. */
data class FetchedDate(val key: String, val label: String, val at: Long)

/** What the source currently knows about a title; [sequels] are AniList follow-ups (e.g. the next season). */
data class Fetched(
    val title: String, val format: String, val status: String, val dates: List<FetchedDate>, val sequels: List<Long> = emptyList(),
    val premiered: String = "", val imageUrl: String? = null
)

/**
 * AniList (anime, GraphQL) and TVmaze (series, REST), both free and without a key. The parsing is separate
 * from the network calls so it can be tested with saved answers.
 */
object ReleaseApi {
    private const val ANILIST = "https://graphql.anilist.co"
    private const val TVMAZE = "https://api.tvmaze.com"

    fun search(source: Source, query: String): List<SearchHit> = when (source) {
        Source.ANILIST -> parseAniListSearch(
            post(ANILIST, aniListBody(
                "query(\$s:String){Page(perPage:20){media(search:\$s,type:ANIME,sort:SEARCH_MATCH){id title{romaji english} format status startDate{year month day} coverImage{medium}}}}",
                JSONObject().put("s", query)
            ))
        )
        Source.TVMAZE -> parseTvMazeSearch(get("$TVMAZE/search/shows?q=" + URLEncoder.encode(query, "UTF-8")))
    }

    fun fetch(source: Source, id: Long, zone: ZoneId = ZoneId.systemDefault()): Fetched = when (source) {
        Source.ANILIST -> parseAniListMedia(
            post(ANILIST, aniListBody(
                "query(\$id:Int){Media(id:\$id){id title{romaji english} format status startDate{year month day} coverImage{medium} " +
                    "airingSchedule(notYetAired:true,perPage:50){nodes{airingAt episode}} " +
                    "relations{edges{relationType node{id type status}}}}}",
                JSONObject().put("id", id)
            )),
            zone
        )
        Source.TVMAZE -> parseTvMazeShow(get("$TVMAZE/shows/$id?embed=episodes"), System.currentTimeMillis())
    }

    // --- parsing ---

    private fun aniTitle(m: JSONObject): String {
        val t = m.optJSONObject("title") ?: return ""
        return t.optString("english").takeIf { it.isNotBlank() && it != "null" } ?: t.optString("romaji")
    }

    private fun JSONObject.str(name: String): String = if (isNull(name)) "" else optString(name)

    /** AniList's start date as "2017-10-03", "2017-10" or "2017". */
    private fun aniDate(m: JSONObject): String {
        val d = m.optJSONObject("startDate") ?: return ""
        if (d.isNull("year")) return ""
        return buildString {
            append(d.getInt("year"))
            if (!d.isNull("month")) {
                append("-%02d".format(d.getInt("month")))
                if (!d.isNull("day")) append("-%02d".format(d.getInt("day")))
            }
        }
    }

    private fun aniImage(m: JSONObject): String? = m.optJSONObject("coverImage")?.str("medium")?.takeIf { it.isNotBlank() }

    private fun mazeImage(s: JSONObject): String? = s.optJSONObject("image")?.str("medium")?.takeIf { it.isNotBlank() }?.replace("http://", "https://")

    fun parseAniListSearch(body: String): List<SearchHit> {
        val media = JSONObject(body).getJSONObject("data").getJSONObject("Page").getJSONArray("media")
        return (0 until media.length()).map { i ->
            val m = media.getJSONObject(i)
            SearchHit(Source.ANILIST, m.getLong("id"), aniTitle(m), m.str("format"), m.str("status"), aniDate(m), aniImage(m))
        }
    }

    fun parseAniListMedia(body: String, zone: ZoneId): Fetched {
        val m = JSONObject(body).getJSONObject("data").getJSONObject("Media")
        val format = m.str("format")
        val status = m.str("status")
        val nodes = m.optJSONObject("airingSchedule")?.optJSONArray("nodes") ?: JSONArray()
        val dates = (0 until nodes.length()).map { i ->
            val n = nodes.getJSONObject(i)
            val ep = n.getInt("episode")
            FetchedDate("ep$ep", "E$ep", n.getLong("airingAt") * 1000)
        }.toMutableList()
        // Not started and no episode times yet (or a film): the start day, if it is known exactly
        if (dates.isEmpty() && status == "NOT_YET_RELEASED") {
            m.optJSONObject("startDate")?.let { d ->
                if (!d.isNull("year") && !d.isNull("month") && !d.isNull("day")) {
                    val day = LocalDate.of(d.getInt("year"), d.getInt("month"), d.getInt("day"))
                    dates += FetchedDate("start", "", day.atStartOfDay(zone).toInstant().toEpochMilli())
                }
            }
        }
        val edges = m.optJSONObject("relations")?.optJSONArray("edges") ?: JSONArray()
        val sequels = (0 until edges.length()).mapNotNull { i ->
            val e = edges.getJSONObject(i)
            val node = e.optJSONObject("node") ?: return@mapNotNull null
            node.getLong("id").takeIf { e.str("relationType") == "SEQUEL" && node.str("type") == "ANIME" }
        }
        return Fetched(aniTitle(m), format, status, dates, sequels, aniDate(m), aniImage(m))
    }

    fun parseTvMazeSearch(body: String): List<SearchHit> {
        val arr = JSONArray(body)
        return (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i).getJSONObject("show")
            SearchHit(Source.TVMAZE, s.getLong("id"), s.str("name"), s.str("type"), s.str("status"), s.str("premiered"), mazeImage(s))
        }
    }

    /** Only episodes still to come with an exact time; the rest is history the app already has. */
    fun parseTvMazeShow(body: String, now: Long): Fetched {
        val s = JSONObject(body)
        val eps = s.optJSONObject("_embedded")?.optJSONArray("episodes") ?: JSONArray()
        val dates = (0 until eps.length()).mapNotNull { i ->
            val e = eps.getJSONObject(i)
            val stamp = e.str("airstamp").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val at = runCatching { OffsetDateTime.parse(stamp).toInstant().toEpochMilli() }.getOrNull() ?: return@mapNotNull null
            if (at <= now) return@mapNotNull null
            val season = e.optInt("season")
            val number = if (e.isNull("number")) null else e.optInt("number")
            val code = if (number == null) "S${season}" else "S${season}E$number"
            val name = e.str("name").takeIf { it.isNotBlank() && !it.startsWith("Episode ") }
            FetchedDate("${e.getLong("id")}", if (name != null) "$code · $name" else code, at)
        }
        return Fetched(s.str("name"), s.str("type"), s.str("status"), dates, premiered = s.str("premiered"), imageUrl = mazeImage(s))
    }

    // --- network ---

    private fun aniListBody(query: String, variables: JSONObject) = JSONObject().put("query", query).put("variables", variables).toString()

    private fun get(url: String): String = request(url, null)

    private fun post(url: String, json: String): String = request(url, json)

    private fun request(url: String, json: String?): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "LifeHub (Android)")
            if (json != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(json.toByteArray()) }
            }
            val code = conn.responseCode
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
