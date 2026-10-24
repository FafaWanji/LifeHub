package com.example.lifeorganizer.releases.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where a title comes from: AniList (anime) or TVmaze (series). Both work without an account or key. */
enum class Source { ANILIST, TVMAZE }

/** A film, series or anime on the radar. */
@Entity(tableName = "watch_items", indices = [Index(value = ["source", "sourceId"], unique = true)])
data class WatchItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: Source,
    val sourceId: Long,
    val title: String,
    /** E.g. "TV", "ONA", "MOVIE" (AniList) or "Scripted" (TVmaze); only shown. */
    val format: String = "",
    /** The source's state, e.g. RELEASING / FINISHED or Running / Ended; only shown. */
    val status: String = "",
    /** A notification when an episode or the film is out. */
    val notify: Boolean = true,
    /** Poster, from the source. */
    val imageUrl: String? = null,
    /** First release, "2017-10-03" or only the year; "" = unknown. Sorts as text. */
    @ColumnInfo(defaultValue = "") val premiered: String = "",
    /** Own ranking "S" … "F", null = not ranked. */
    val tier: String? = null,
    /** Last successful update from the source, epoch millis; 0 = never. */
    val lastSync: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * One release (an episode or a film's start) at a known time. Future ones are replaced on every update,
 * so episodes that are no longer announced leave the calendar; past ones stay for a while.
 */
@Entity(
    tableName = "release_dates",
    primaryKeys = ["itemId", "key"],
    indices = [Index("at")],
    foreignKeys = [ForeignKey(entity = WatchItem::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)]
)
data class ReleaseDate(
    val itemId: Long,
    /** Unique per item, e.g. "ep6" or "s2e5". */
    val key: String,
    /** E.g. "Episode 6" or "S2E5 · Woe's Hollow"; the app adds the title. */
    val label: String,
    /** Epoch millis. */
    val at: Long,
    val notified: Boolean = false
)

/** A release with its title, for the calendar and notifications. */
data class ReleaseRow(
    val itemId: Long,
    val key: String,
    val label: String,
    val at: Long,
    val title: String
)
