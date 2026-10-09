package com.example.lifeorganizer.web

import android.content.Context
import androidx.room.InvalidationTracker
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.money.data.MoneyDatabase
import com.example.lifeorganizer.notes.data.NotesDatabase
import java.util.concurrent.atomic.AtomicLong

/**
 * Counts changes in the calendar, notes and finance tables. The browser polls the counter and reloads
 * when it moved, so edits on the phone show up in the browser within seconds (and the other way round).
 */
class ChangeTracker(context: Context) {
    val version = AtomicLong(System.currentTimeMillis())

    private val observers = listOf(
        AppDatabase.getDatabase(context).invalidationTracker to arrayOf("events", "reminders", "categories"),
        NotesDatabase.getDatabase(context).invalidationTracker to arrayOf("notes", "note_labels", "note_label_cross_ref"),
        MoneyDatabase.getDatabase(context).invalidationTracker to arrayOf("transactions", "money_categories", "recurring")
    ).map { (tracker, tables) ->
        val observer = object : InvalidationTracker.Observer(tables) {
            override fun onInvalidated(tables: Set<String>) { version.incrementAndGet() }
        }
        tracker.addObserver(observer)
        tracker to observer
    }

    fun close() = observers.forEach { (tracker, observer) -> tracker.removeObserver(observer) }
}
