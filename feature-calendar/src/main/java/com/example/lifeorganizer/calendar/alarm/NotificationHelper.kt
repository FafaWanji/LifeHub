package com.example.lifeorganizer.calendar.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.first
import com.example.lifeorganizer.calendar.R

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Notifications are built outside Compose, so read the app language directly.
    private val lang: String = kotlinx.coroutines.runBlocking {
        com.example.lifeorganizer.core.settings.SettingsManager(context.applicationContext).languageCode.first()
    }

    init {
        // Channels must exist before posting, otherwise Android silently drops the notification.
        // Creating an existing channel is a no-op, so doing it here every time is safe.
        createNotificationChannel()
        createBirthdayNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                com.example.lifeorganizer.core.i18n.Str.remindersChannel.of(java.util.Locale.getDefault().language),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = com.example.lifeorganizer.core.i18n.Str.remindersChannelDesc.of(java.util.Locale.getDefault().language)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createBirthdayNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BIRTHDAY_CHANNEL_ID,
                com.example.lifeorganizer.core.i18n.Str.birthdayChannel.of(java.util.Locale.getDefault().language),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Birthday reminders at 23:59 with sound and vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        soundUri: String? = null,
        linkedNoteId: Long? = null
    ) {
        val openIntent = Intent(context, Class.forName("com.example.lifeorganizer.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NOTIFICATION_OPENED", true)
            // Note reminders open the linked note directly.
            if (linkedNoteId != null) {
                action = ACTION_OPEN_NOTE
                putExtra(EXTRA_NOTE_ID, linkedNoteId)
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("EVENT_TITLE", title)
            putExtra("REMINDER_TYPE", message)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_notification, com.example.lifeorganizer.core.i18n.Str.snooze.of(lang), snoozePendingIntent)
            .addAction(R.drawable.ic_notification, com.example.lifeorganizer.core.i18n.Str.dismiss.of(lang), dismissPendingIntent)

        if (soundUri != null) {
            notificationBuilder.setSound(android.net.Uri.parse(soundUri))
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        } else {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun showBirthdayNotification(title: String, message: String, notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()) {
        val openIntent = Intent(context, Class.forName("com.example.lifeorganizer.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NOTIFICATION_OPENED", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BIRTHDAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_notification, com.example.lifeorganizer.core.i18n.Str.dismiss.of(lang), dismissPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    companion object {
        const val CHANNEL_ID = "calendar_reminders"
        const val BIRTHDAY_CHANNEL_ID = "birthday_reminders"
        const val ACTION_SNOOZE = "com.example.lifeorganizer.calendar.SNOOZE"
        const val ACTION_DISMISS = "com.example.lifeorganizer.calendar.DISMISS"
        const val ACTION_OPEN_NOTE = "com.example.lifeorganizer.ACTION_OPEN_NOTE"
        const val EXTRA_NOTE_ID = "NOTE_ID"
    }
}

