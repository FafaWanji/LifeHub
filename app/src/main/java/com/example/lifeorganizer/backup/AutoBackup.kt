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
 * (Android/data/<package>/files/backups), keeping the newest [keep] files.
 */
object AutoBackup {
    private const val WORK_NAME = "auto_backup"
    private const val PREFS = "auto_backup"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_FOLDER = "folder"
    /** How many auto backups stay (backup settings, 5 by default). */
    fun keep(context: Context): Int =
        com.example.lifeorganizer.core.settings.MicroSettings.get(context, com.example.lifeorganizer.core.settings.MicroSettings.BACKUP_KEEP).coerceIn(1, 50)

    /**
     * Android/data/<package>/files/backups, or internal storage when that folder is not writable
     * (it can be left over from an earlier installation and belong to another user id).
     */
    fun folder(context: Context): File {
        val external = context.getExternalFilesDir(null)?.let { File(it, "backups") }
        if (external != null && external.apply { mkdirs() }.let { writable(it) }) return external
        return File(context.filesDir, "backups").apply { mkdirs() }
    }

    private fun writable(dir: File): Boolean = runCatching {
        val probe = File(dir, ".probe")
        probe.writeText("")
        probe.delete()
    }.isSuccess

    fun isEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        schedule(context)
    }

    /** Extra target folder chosen by the user (e.g. Google Drive), kept across reinstalls of the app. */
    fun externalFolder(context: Context): android.net.Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_FOLDER, null)?.let(android.net.Uri::parse)

    fun setExternalFolder(context: Context, tree: android.net.Uri?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        externalFolder(context)?.let { old ->
            runCatching { context.contentResolver.releasePersistableUriPermission(old, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        }
        if (tree != null) {
            context.contentResolver.takePersistableUriPermission(tree, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        prefs.edit().putString(KEY_FOLDER, tree?.toString()).apply()
    }

    /** Copies [file] into the chosen folder and keeps the newest [keep] auto backups there. */
    private fun copyToExternal(context: Context, file: File) {
        val tree = externalFolder(context) ?: return
        val resolver = context.contentResolver
        val parent = android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, android.provider.DocumentsContract.getTreeDocumentId(tree))
        val children = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(tree, android.provider.DocumentsContract.getTreeDocumentId(tree))
        val existing = mutableListOf<Pair<String, android.net.Uri>>()
        resolver.query(children, arrayOf(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID, android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (name.startsWith("lifehub-auto-")) existing += name to android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            }
        }
        // Same day again: overwrite instead of piling up copies
        existing.firstOrNull { it.first == file.name }?.let { runCatching { android.provider.DocumentsContract.deleteDocument(resolver, it.second) } }
        val target = android.provider.DocumentsContract.createDocument(resolver, parent, "application/json", file.name) ?: return
        resolver.openOutputStream(target, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } }
        existing.filter { it.first != file.name }.sortedByDescending { it.first }.drop(keep(context) - 1)
            .forEach { runCatching { android.provider.DocumentsContract.deleteDocument(resolver, it.second) } }
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
        // Wi-Fi only (backup settings) matters for the copy to an online folder such as Google Drive
        val wifiOnly = com.example.lifeorganizer.core.settings.MicroSettings.get(context, com.example.lifeorganizer.core.settings.MicroSettings.BACKUP_WIFI_ONLY)
        val constraints = Constraints.Builder().setRequiresStorageNotLow(true)
            .apply { if (wifiOnly) setRequiredNetworkType(androidx.work.NetworkType.UNMETERED) }
            .build()
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        // UPDATE keeps the weekly rhythm but takes over changed constraints
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Writes one backup now and removes the oldest beyond [keep]. */
    suspend fun runNow(context: Context): File {
        val dir = folder(context)
        var file = File(dir, "lifehub-auto-${LocalDate.now()}.json")
        val stream = try {
            file.outputStream()
        } catch (e: java.io.IOException) {
            // A single leftover file of an earlier installation cannot be overwritten: use another name
            file = File(dir, "lifehub-auto-${LocalDate.now()}-${System.currentTimeMillis() % 100000}.json")
            file.outputStream()
        }
        stream.use { BackupManager(context).export(it) }
        // Old ones go in both possible folders (the app falls back to internal storage when the external one is not
        // writable, so earlier backups can sit in the other one)
        listOfNotNull(context.getExternalFilesDir(null)?.let { File(it, "backups") }, File(context.filesDir, "backups"))
            .flatMap { d -> d.listFiles { f -> f.name.startsWith("lifehub-auto-") && f.name.endsWith(".json") }.orEmpty().toList() }
            .sortedByDescending { it.lastModified() }
            .drop(keep(context))
            .forEach { it.delete() }
        // The other copies are best effort: a revoked folder must not fail the local backup.
        runCatching { copyToExternal(context, file) }
        runCatching { copyToDocuments(context, file) }
        return file
    }

    /** Public folder for the copy that stays when the app is uninstalled. */
    const val DOCUMENTS_FOLDER = "Documents/LifeHub"

    /**
     * Copies [file] to Documents/LifeHub (Android 10+, no permission needed), which survives uninstalling the app, and
     * keeps the newest [keep] there. Copies of an earlier installation belong to it: Android hides them from this one,
     * so they are left alone – they can still be picked when restoring.
     */
    private fun copyToDocuments(context: Context, file: File) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return
        val resolver = context.contentResolver
        val collection = android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val name = android.provider.MediaStore.MediaColumns.DISPLAY_NAME
        val path = android.provider.MediaStore.MediaColumns.RELATIVE_PATH
        // Our copies there, newest first
        val ours = mutableListOf<Pair<String, android.net.Uri>>()
        resolver.query(
            collection, arrayOf(android.provider.BaseColumns._ID, name),
            "$path LIKE ? AND $name LIKE ?", arrayOf("$DOCUMENTS_FOLDER%", "lifehub-auto-%"), "$name DESC"
        )?.use { c ->
            while (c.moveToNext()) ours += c.getString(1) to android.content.ContentUris.withAppendedId(collection, c.getLong(0))
        }
        // Same day again: replace instead of piling up copies
        ours.filter { it.first == file.name }.forEach { runCatching { resolver.delete(it.second, null, null) } }
        val values = android.content.ContentValues().apply {
            put(name, file.name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(path, "$DOCUMENTS_FOLDER/")
        }
        val target = resolver.insert(collection, values) ?: return
        resolver.openOutputStream(target, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } }
        ours.filter { it.first != file.name }.drop(keep(context) - 1)
            .forEach { runCatching { resolver.delete(it.second, null, null) } }
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
