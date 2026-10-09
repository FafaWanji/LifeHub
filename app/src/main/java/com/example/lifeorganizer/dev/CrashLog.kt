package com.example.lifeorganizer.dev

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.lifeorganizer.BuildConfig
import com.example.lifeorganizer.MainActivity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** One saved crash: file name is the time, content the full report. */
data class CrashEntry(val file: File, val time: String, val summary: String, val report: String)

/**
 * Every uncaught exception (also from widgets, alarms and workers) is written to files/crashes
 * together with the recent system log, and announced with a silent notification that opens
 * the developer screen.
 */
object CrashLog {
    private const val CHANNEL_ID = "dev_crash_log"
    private const val NOTIFICATION_ID = 7_331
    private const val KEEP = 30
    private val fileTime = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    /** Called once from the Application, so crashes before any screen opens are caught too. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (DevMode.crashLog.value) {
                runCatching { record(app, thread, throwable) }.onFailure { Log.e("LifeHub", "Crash log failed", it) }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun deviceInfo(): String =
        "LifeHub ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
            "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}"

    /** Device, version and the full stack trace – everything needed without asking back. */
    fun report(thread: Thread, throwable: Throwable): String =
        deviceInfo() + "\n${LocalDateTime.now().withNano(0)}, thread ${thread.name}\n\n" + throwable.stackTraceToString()

    /** "NullPointerException: x was null" of the innermost cause, plus where in LifeHub it happened. */
    fun summary(throwable: Throwable): String {
        var root = throwable
        while (root.cause != null && root.cause !== root) root = root.cause!!
        val headline = root.javaClass.simpleName + (root.message?.let { ": " + it.take(160) } ?: "")
        val ownFrame = generateSequence(throwable) { it.cause }.flatMap { it.stackTrace.asSequence() }
            .firstOrNull { it.className.startsWith("com.example.lifeorganizer") }
        return if (ownFrame == null) headline
        else "$headline\n@ ${ownFrame.fileName ?: ownFrame.className.substringAfterLast('.')}:${ownFrame.lineNumber}"
    }

    /** This app's own system log (no permission needed for the own process). */
    fun systemLog(lines: Int): String = runCatching {
        val p = ProcessBuilder("logcat", "-d", "-v", "time", "-t", lines.toString(), "--pid=${android.os.Process.myPid()}")
            .redirectErrorStream(true).start()
        p.inputStream.bufferedReader().use { it.readText() }
    }.getOrElse { "(system log unavailable: ${it.message})" }

    private fun dir(context: Context) = File(context.filesDir, "crashes").apply { mkdirs() }

    private fun record(context: Context, thread: Thread, throwable: Throwable) {
        val summary = summary(throwable)
        val file = File(dir(context), "${LocalDateTime.now().format(fileTime)}.txt")
        // First line holds the summary so the list can show it without parsing the trace
        file.writeText(summary.replace('\n', ' ') + "\n" + report(thread, throwable))
        // What happened right before the crash
        file.appendText("\n\n--- System log ---\n" + systemLog(200))
        entries(context).drop(KEEP).forEach { it.file.delete() }
        notify(context, summary)
    }

    fun entries(context: Context): List<CrashEntry> =
        dir(context).listFiles { f -> f.extension == "txt" }.orEmpty().sortedByDescending { it.name }.map { f ->
            val text = f.readText()
            val time = runCatching {
                LocalDateTime.parse(f.nameWithoutExtension, fileTime).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
            }.getOrDefault(f.nameWithoutExtension)
            CrashEntry(f, time, text.substringBefore('\n'), text.substringAfter('\n'))
        }

    fun clear(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun notify(context: Context, summary: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        // Silent: shows up in the shade, but no sound and no vibration
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Crash log", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
            }
        )
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_DEV_MODE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.lifeorganizer.calendar.R.drawable.ic_notification)
            .setContentTitle("LifeHub crash: " + summary.substringBefore(':').substringBefore('\n'))
            .setContentText(summary.substringAfter(": ", summary).substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setSilent(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }
}
