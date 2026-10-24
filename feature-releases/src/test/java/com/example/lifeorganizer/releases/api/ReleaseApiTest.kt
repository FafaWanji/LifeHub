package com.example.lifeorganizer.releases.api

import com.example.lifeorganizer.releases.data.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReleaseApiTest {
    private val zone = ZoneId.of("Europe/Berlin")

    @Test fun aniListSearch() {
        val body = """{"data":{"Page":{"media":[{"id":210482,"title":{"romaji":"JoJo no Kimyou na Bouken: Steel Ball Run","english":null},"format":"ONA","status":"RELEASING","startDate":{"year":2026}}]}}}"""
        assertEquals(
            listOf(SearchHit(Source.ANILIST, 210482, "JoJo no Kimyou na Bouken: Steel Ball Run", "ONA", "RELEASING", "2026")),
            ReleaseApi.parseAniListSearch(body)
        )
    }

    @Test fun aniListEpisodesAndSequel() {
        val body = """{"data":{"Media":{"id":1,"title":{"romaji":"R","english":"Steel Ball Run"},"format":"ONA","status":"RELEASING",
            "startDate":{"year":2026,"month":9,"day":25},
            "airingSchedule":{"nodes":[{"airingAt":1793347200,"episode":6},{"airingAt":1793952000,"episode":7}]},
            "relations":{"edges":[{"relationType":"SEQUEL","node":{"id":99,"type":"ANIME","status":"NOT_YET_RELEASED"}},
                                  {"relationType":"SEQUEL","node":{"id":98,"type":"MANGA","status":"FINISHED"}},
                                  {"relationType":"PREQUEL","node":{"id":97,"type":"ANIME","status":"FINISHED"}}]}}}}"""
        val f = ReleaseApi.parseAniListMedia(body, zone)
        assertEquals("Steel Ball Run", f.title)
        assertEquals(listOf(FetchedDate("ep6", "E6", 1793347200000), FetchedDate("ep7", "E7", 1793952000000)), f.dates)
        assertEquals(listOf(99L), f.sequels)
        assertEquals("2026-09-25", f.premiered)
    }

    @Test fun aniListFilmWithOnlyAStartDay() {
        val body = """{"data":{"Media":{"id":1,"title":{"english":"Film"},"format":"MOVIE","status":"NOT_YET_RELEASED",
            "startDate":{"year":2027,"month":3,"day":5},"airingSchedule":{"nodes":[]},"relations":{"edges":[]}}}}"""
        val f = ReleaseApi.parseAniListMedia(body, zone)
        assertEquals(listOf(FetchedDate("start", "", LocalDate.of(2027, 3, 5).atStartOfDay(zone).toInstant().toEpochMilli())), f.dates)
    }

    @Test fun aniListFinishedHasNoDates() {
        val body = """{"data":{"Media":{"id":1,"title":{"english":"Old"},"format":"TV","status":"FINISHED",
            "startDate":{"year":2020,"month":1,"day":1},"airingSchedule":{"nodes":[]},"relations":{"edges":[]}}}}"""
        assertTrue(ReleaseApi.parseAniListMedia(body, zone).dates.isEmpty())
    }

    @Test fun animeWithoutFutureEpisodesContinuesFromAniList() {
        val show = """{"id":90817,"name":"Steel Ball Run","language":"Japanese","genres":["Anime"],"status":"Running","_embedded":{"episodes":[
            {"id":1,"season":2,"number":4,"name":"x","airstamp":"2026-10-16T12:00:00+00:00"},
            {"id":2,"season":2,"number":5,"name":"Episode 5","airstamp":"2026-10-23T12:00:00+00:00"}]}}"""
        assertTrue(ReleaseApi.isRunningAnime(show))
        val last = ReleaseApi.parseTvMazeLast(show)!!
        assertEquals(ReleaseApi.LastEpisode(2, 5, java.time.Instant.parse("2026-10-23T12:00:00Z").toEpochMilli()), last)
        // 1793347200 = 30 Oct 08:00 UTC, one week later the next
        val ani = """{"data":{"Page":{"media":[{"id":210482,"airingSchedule":{"nodes":[{"airingAt":1793952000,"episode":7},{"airingAt":1793347200,"episode":6}]}}]}}}"""
        val now = java.time.Instant.parse("2026-10-24T15:00:00Z").toEpochMilli()
        assertEquals(
            listOf(FetchedDate("al6", "S2E6", 1793347200000), FetchedDate("al7", "S2E7", 1793952000000)),
            ReleaseApi.continueFromAniList(ani, last, now)
        )
    }

    @Test fun tvMazeOnlyFutureEpisodes() {
        val body = """{"id":44933,"name":"Severance","type":"Scripted","status":"Running","_embedded":{"episodes":[
            {"id":1,"season":2,"number":9,"name":"The After Hours","airstamp":"2025-03-14T01:00:00+00:00"},
            {"id":2,"season":3,"number":1,"name":"Episode 1","airstamp":"2027-01-15T02:00:00+00:00"},
            {"id":3,"season":3,"number":2,"name":"Cold Harbor","airstamp":"2027-01-22T02:00:00+00:00"},
            {"id":4,"season":3,"number":null,"name":"Special","airstamp":null}]}}"""
        val now = java.time.Instant.parse("2026-10-24T00:00:00Z").toEpochMilli()
        val f = ReleaseApi.parseTvMazeShow(body, now)
        assertEquals(null, f.imageUrl)
        assertEquals(
            listOf(
                FetchedDate("2", "S3E1", java.time.Instant.parse("2027-01-15T02:00:00Z").toEpochMilli()),
                FetchedDate("3", "S3E2 · Cold Harbor", java.time.Instant.parse("2027-01-22T02:00:00Z").toEpochMilli())
            ),
            f.dates
        )
    }
}
