package com.example.lifeorganizer.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.lifeorganizer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A newer release on GitHub. */
data class UpdateInfo(val version: String, val notes: String, val apkUrl: String)

/**
 * Checks the latest GitHub release of the public repository and installs its APK.
 * The release must carry an .apk asset signed with the same key as the installed app.
 */
object Updater {
    private const val LATEST = "https://api.github.com/repos/FafaWanji/LifeOrganizer/releases/latest"

    /** Null when the app is up to date or the check failed. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            conn.use { c ->
                if (c.responseCode != 200) return@runCatching null
                val json = JSONObject(c.inputStream.bufferedReader().readText())
                val version = json.optString("tag_name").removePrefix("v")
                if (!isNewer(version, BuildConfig.VERSION_NAME)) return@runCatching null
                val assets = json.optJSONArray("assets") ?: return@runCatching null
                val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") } ?: return@runCatching null
                UpdateInfo(version, json.optString("body"), apk.optString("browser_download_url"))
            }
        }.getOrNull()
    }

    /** Compares dotted versions numerically ("0.10" > "0.9"). */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Downloads the APK into the app's files (reporting 0..1 progress) and returns it. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "LifeOrganizer-${info.version}.apk")
        val conn = URL(info.apkUrl).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.use { c ->
            val total = c.contentLengthLong.takeIf { it > 0 }
            c.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        total?.let { t -> withContext(Dispatchers.Main) { onProgress(done.toFloat() / t) } }
                    }
                }
            }
        }
        target
    }

    /** Hands the APK to Android's package installer (the user confirms there). */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try { block(this) } finally { disconnect() }
}
