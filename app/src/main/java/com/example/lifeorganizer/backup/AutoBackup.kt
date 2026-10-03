package com.example.lifeorganizer.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Weekly full backup into the app's external files folder
 * (Android/data/<package>/files/backups), keeping the newest [KEEP] files.
 */
object AutoBackup {
    private const val WORK_NAME = "auto_backup"
    private const val PREFS = "auto_backup"
    private const val KEY_ENABLED = "enabled"
    const val KEEP = 5

    fun folder(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "backups").apply { mkdirs() }

    fun isEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        schedule(context)
    }

    /** Newest backup file, or null if none was written yet. */
    fun latest(context: Context): File? =
        folder(context).listFiles { f -> f.name.endsWith(".json") }?.maxByOrNull { it.lastModified() }

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        if (!isEnabled(context)) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Writes one backup now and removes the oldest beyond [KEEP]. */
    suspend fun runNow(context: Context): File {
        val dir = folder(context)
        val file = File(dir, "lifeorganizer-auto-${LocalDate.now()}.json")
        file.outputStream().use { BackupManager(context).export(it) }
        dir.listFiles { f -> f.name.startsWith("lifeorganizer-auto-") && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KEEP)
            ?.forEach { it.delete() }
        return file
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        AutoBackup.runNow(applicationContext)
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
