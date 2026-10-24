package com.example.lifeorganizer.releases.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lifeorganizer.core.settings.AppModule
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.core.settings.enabledModules
import com.example.lifeorganizer.releases.R
import com.example.lifeorganizer.releases.api.ReleaseApi
import com.example.lifeorganizer.releases.data.ReleaseDatabase
import com.example.lifeorganizer.releases.data.ReleaseDate
import com.example.lifeorganizer.releases.data.ReleaseRow
import com.example.lifeorganizer.releases.data.Source
import com.example.lifeorganizer.releases.data.WatchItem
import com.example.lifeorganizer.releases.ui.ReleaseStr
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Updates the dates from the sources and announces what is out. */
object ReleaseSync {
    const val ACTION_OPEN_RELEASES = "com.example.lifeorganizer.ACTION_RELEASES"
    private const val CHANNEL = "releases"
    private const val PERIODIC = "release_sync"
    private const val NEXT = "release_next"
    /** Past releases stay in the calendar this long. */
    private const val KEEP_PAST_MS = 60L * 24 * 60 * 60 * 1000
    /** Not announced any more when found later than this (phone off for days, first sync). */
    private const val LATE_MS = 12L * 60 * 60 * 1000

    /** Twice a day an update; a one-off wake-up right at the next release. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReleaseWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private suspend fun scheduleNext(context: Context) {
        val now = System.currentTimeMillis()
        val next = ReleaseDatabase.get(context).dao().nextToNotify(now)
        val wm = WorkManager.getInstance(context)
        if (next == null) { wm.cancelUniqueWork(NEXT); return }
        val request = OneTimeWorkRequestBuilder<ReleaseWorker>()
            .setInitialDelay(next - now + 30_000, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(NEXT, ExistingWorkPolicy.REPLACE, request)
    }

    /** Fetches one title and stores its dates. False when the source could not be reached. */
    suspend fun refresh(context: Context, item: WatchItem): Boolean {
        val dao = ReleaseDatabase.get(context).dao()
        val fetched = runCatching { ReleaseApi.fetch(item.source, item.sourceId) }.getOrElse { return false }
        val now = System.currentTimeMillis()
        dao.replaceFuture(item.id, fetched.dates.filter { it.at > now }.map { ReleaseDate(item.id, it.key, it.label, it.at) }, now)
        dao.update(item.copy(title = fetched.title.ifBlank { item.title }, format = fetched.format, status = fetched.status, lastSync = now))
        // A finished anime with an announced follow-up: the next season goes on the radar by itself
        if (item.source == Source.ANILIST && fetched.status == "FINISHED") {
            for (id in fetched.sequels) {
                if (dao.find(Source.ANILIST, id) != null) continue
                val sequel = runCatching { ReleaseApi.fetch(Source.ANILIST, id) }.getOrNull() ?: continue
                if (sequel.status != "NOT_YET_RELEASED" && sequel.status != "RELEASING") continue
                val newId = dao.insert(WatchItem(source = Source.ANILIST, sourceId = id, title = sequel.title, format = sequel.format, status = sequel.status, notify = item.notify))
                if (newId > 0) refresh(context, dao.find(Source.ANILIST, id) ?: continue)
            }
        }
        return true
    }

    /** All titles; true when every one could be updated. */
    suspend fun refreshAll(context: Context): Boolean {
        val dao = ReleaseDatabase.get(context).dao()
        var ok = true
        dao.itemsSync().forEach { if (!refresh(context, it)) ok = false }
        dao.deleteOlderThan(System.currentTimeMillis() - KEEP_PAST_MS)
        scheduleNext(context)
        return ok
    }

    /** Notifications for what came out since the last check, then the wake-up for the next one. */
    suspend fun announce(context: Context) {
        val dao = ReleaseDatabase.get(context).dao()
        val now = System.currentTimeMillis()
        val settings = SettingsManager(context)
        val moduleOn = AppModule.RELEASES in settings.enabledModules.first()
        val lang = settings.languageCode.first()
        dao.due(now, now - LATE_MS).forEach { row ->
            if (moduleOn) notify(context, lang, row)
            dao.markNotified(row.itemId, row.key)
        }
        scheduleNext(context)
    }

    /** "Episode 6" for "E6", the TVmaze code as it is, "Start" for a start day. */
    fun label(label: String, lang: String): String = when {
        label.isEmpty() -> ReleaseStr.start.of(lang)
        label.matches(Regex("E\\d+")) -> ReleaseStr.episode.of(lang).format(label.drop(1))
        else -> label
    }

    private fun notify(context: Context, lang: String, row: ReleaseRow) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL, ReleaseStr.channel.of(lang), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, Class.forName("com.example.lifeorganizer.MainActivity"))
                .setAction(ACTION_OPEN_RELEASES).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val what = if (row.label.isEmpty()) row.title else "${row.title} – ${label(row.label, lang)}"
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_release_notification)
            .setContentTitle(ReleaseStr.isOut.of(lang).format(what))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify("release_${row.itemId}_${row.key}".hashCode(), notification) }
    }
}

/** Updates (catches moved dates; offline it keeps the stored ones), then announces what is out. */
class ReleaseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ReleaseSync.refreshAll(applicationContext)
        ReleaseSync.announce(applicationContext)
        return Result.success()
    }
}
