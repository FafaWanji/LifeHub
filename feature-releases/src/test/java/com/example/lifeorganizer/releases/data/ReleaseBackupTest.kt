package com.example.lifeorganizer.releases.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseBackupTest {
    /** Only what the backup uses; insert behaves like the unique (source, sourceId) index with IGNORE. */
    private class FakeDao(val list: MutableList<WatchItem> = mutableListOf()) : ReleaseDao {
        override fun items(): Flow<List<WatchItem>> = flowOf(list)
        override suspend fun itemsSync() = list.toList()
        override suspend fun find(source: Source, sourceId: Long) = list.firstOrNull { it.source == source && it.sourceId == sourceId }
        override suspend fun insert(item: WatchItem): Long {
            if (find(item.source, item.sourceId) != null) return -1
            list += item.copy(id = list.size + 1L)
            return list.size.toLong()
        }
        override suspend fun update(item: WatchItem) = Unit
        override suspend fun delete(item: WatchItem) = Unit
        override fun releases(): Flow<List<ReleaseRow>> = flowOf(emptyList())
        override suspend fun due(now: Long, since: Long) = emptyList<ReleaseRow>()
        override suspend fun nextToNotify(now: Long): Long? = null
        override suspend fun markNotified(itemId: Long, key: String) = Unit
        override suspend fun deleteFuture(itemId: Long, now: Long) = Unit
        override suspend fun deleteOlderThan(before: Long) = Unit
        override suspend fun insertDates(dates: List<ReleaseDate>) = Unit
    }

    @Test fun roundTripAddsOnlyMissingTitles() = runBlocking {
        val source = FakeDao(mutableListOf(
            WatchItem(1, Source.ANILIST, 210482, "JoJo", format = "ONA", tier = "S", imageUrl = null, notify = false, createdAt = 5),
            WatchItem(2, Source.TVMAZE, 44933, "Severance", premiered = "2022-02-18", imageUrl = "https://x/p.jpg", createdAt = 6)
        ))
        // Round-trip through text as in the backup file
        val json = JSONObject(ReleaseBackup.export(source).toString())

        val target = FakeDao(mutableListOf(WatchItem(1, Source.TVMAZE, 44933, "Severance (own)", tier = "A")))
        assertEquals(1, ReleaseBackup.import(target, json))

        val jojo = target.list.single { it.sourceId == 210482L }
        assertEquals(WatchItem(2, Source.ANILIST, 210482, "JoJo", format = "ONA", tier = "S", imageUrl = null, notify = false, createdAt = 5), jojo)
        // The title already on the radar stays untouched
        assertEquals("Severance (own)", target.list.single { it.sourceId == 44933L }.title)
    }

    @Test fun unknownSourceIsSkipped() = runBlocking {
        val json = JSONObject("""{"items":[{"source":"NETFLIX","sourceId":1,"title":"X"}]}""")
        assertEquals(0, ReleaseBackup.import(FakeDao(), json))
    }
}
