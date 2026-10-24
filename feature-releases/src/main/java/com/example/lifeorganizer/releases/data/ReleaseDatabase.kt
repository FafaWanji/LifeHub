package com.example.lifeorganizer.releases.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {
    @Query("SELECT * FROM watch_items ORDER BY title COLLATE NOCASE")
    fun items(): Flow<List<WatchItem>>

    @Query("SELECT * FROM watch_items")
    suspend fun itemsSync(): List<WatchItem>

    @Query("SELECT * FROM watch_items WHERE source = :source AND sourceId = :sourceId")
    suspend fun find(source: Source, sourceId: Long): WatchItem?

    /** -1 when the title is already on the radar. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: WatchItem): Long

    @Update
    suspend fun update(item: WatchItem)

    @Delete
    suspend fun delete(item: WatchItem)

    @Query("SELECT d.itemId, d.`key`, d.label, d.at, w.title FROM release_dates d JOIN watch_items w ON w.id = d.itemId ORDER BY d.at")
    fun releases(): Flow<List<ReleaseRow>>

    /** Released (at or before [now]), not yet announced, of titles that notify. */
    @Query(
        "SELECT d.itemId, d.`key`, d.label, d.at, w.title FROM release_dates d JOIN watch_items w ON w.id = d.itemId " +
            "WHERE w.notify = 1 AND d.notified = 0 AND d.at <= :now AND d.at > :since ORDER BY d.at"
    )
    suspend fun due(now: Long, since: Long): List<ReleaseRow>

    /** The next release still to announce, to wake up right then. */
    @Query("SELECT MIN(d.at) FROM release_dates d JOIN watch_items w ON w.id = d.itemId WHERE w.notify = 1 AND d.notified = 0 AND d.at > :now")
    suspend fun nextToNotify(now: Long): Long?

    @Query("UPDATE release_dates SET notified = 1 WHERE itemId = :itemId AND `key` = :key")
    suspend fun markNotified(itemId: Long, key: String)

    @Query("DELETE FROM release_dates WHERE itemId = :itemId AND at > :now")
    suspend fun deleteFuture(itemId: Long, now: Long)

    @Query("DELETE FROM release_dates WHERE at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDates(dates: List<ReleaseDate>)

    /** The source's current dates replace the announced future ones; past ones (and their "notified") stay. */
    @Transaction
    suspend fun replaceFuture(itemId: Long, dates: List<ReleaseDate>, now: Long) {
        deleteFuture(itemId, now)
        insertDates(dates)
    }
}

class ReleaseConverters {
    @TypeConverter fun fromSource(s: Source): String = s.name
    @TypeConverter fun toSource(s: String): Source = Source.valueOf(s)
}

@Database(entities = [WatchItem::class, ReleaseDate::class], version = 2, exportSchema = true)
@TypeConverters(ReleaseConverters::class)
abstract class ReleaseDatabase : RoomDatabase() {
    abstract fun dao(): ReleaseDao

    companion object {
        @Volatile private var instance: ReleaseDatabase? = null

        /** Poster, first release and tier; filled in by the next update. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_items ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE watch_items ADD COLUMN premiered TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE watch_items ADD COLUMN tier TEXT")
            }
        }

        fun get(context: Context): ReleaseDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ReleaseDatabase::class.java, "releases.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
