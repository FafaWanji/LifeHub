package com.example.lifeorganizer.releases.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Release radar part of the LifeHub backup: only the titles. Their dates come from the source on the next update.
 * Import adds titles that are not on the radar yet; existing ones stay as they are.
 */
object ReleaseBackup {
    suspend fun export(dao: ReleaseDao): JSONObject = JSONObject()
        .put("items", JSONArray(dao.itemsSync().map { i ->
            JSONObject().put("source", i.source.name).put("sourceId", i.sourceId).put("title", i.title)
                .put("format", i.format).put("status", i.status).put("notify", i.notify)
                .put("imageUrl", i.imageUrl).put("premiered", i.premiered).put("tier", i.tier)
                .put("createdAt", i.createdAt)
        }))

    /** Returns how many titles were added. */
    suspend fun import(dao: ReleaseDao, json: JSONObject): Int {
        var added = 0
        val items = json.optJSONArray("items") ?: return 0
        for (k in 0 until items.length()) {
            val o = items.optJSONObject(k) ?: continue
            // A source this version does not know is skipped instead of failing the whole import
            val source = runCatching { Source.valueOf(o.getString("source")) }.getOrNull() ?: continue
            val sourceId = o.optLong("sourceId", -1).takeIf { it >= 0 } ?: continue
            val item = WatchItem(
                source = source,
                sourceId = sourceId,
                title = o.optString("title"),
                format = o.optString("format"),
                status = o.optString("status"),
                notify = o.optBoolean("notify", true),
                imageUrl = o.optString("imageUrl").takeIf { o.has("imageUrl") && !o.isNull("imageUrl") },
                premiered = o.optString("premiered"),
                tier = o.optString("tier").takeIf { o.has("tier") && !o.isNull("tier") },
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
            // Unique (source, sourceId): IGNORE leaves a title that is already there untouched
            if (dao.insert(item) != -1L) added++
        }
        return added
    }
}
