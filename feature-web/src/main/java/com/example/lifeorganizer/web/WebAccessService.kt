package com.example.lifeorganizer.web

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.lifeorganizer.core.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.Inet4Address
import java.net.NetworkInterface

data class WebAccessState(
    val running: Boolean = false,
    val address: String? = null,
    val nameAddress: String? = null,
    val code: String = "",
    val failed: Boolean = false
)

/** Keeps the local web server alive while PC access is on; shows address and code in a notification. */
class WebAccessService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: WebServer? = null
    private var mdns: MdnsResponder? = null
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null
    private var hostName = WebSettings.DEFAULT_NAME
    private var lang = "en"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_NEW_CODE) {
            server?.auth?.newCode()
            publish()
            return START_NOT_STICKY
        }
        if (server != null) return START_NOT_STICKY

        lang = runBlocking { SettingsManager(this@WebAccessService).languageCode.first() }
        createChannel()
        startInForeground(buildNotification("…", ""))
        val settings = WebSettings(this)
        hostName = settings.name
        val started = runCatching { WebServer(this, settings.port).also { it.start() } }
        server = started.getOrNull()
        if (server == null) {
            _state.value = WebAccessState(failed = true)
            stopSelf()
            return START_NOT_STICKY
        }
        startMdns()
        publish()
        scope.launch {
            while (isActive) {
                delay(2_000)
                val s = server ?: break
                if (System.currentTimeMillis() - s.lastActivity > IDLE_LIMIT_MS) { stopSelf(); break }
                // The code changes after a pairing: keep notification and screen up to date
                if (s.auth.code != _state.value.code || localAddress() != _state.value.address) publish()
            }
        }
        return START_NOT_STICKY
    }

    /** Answers "<name>.local" in the Wi-Fi; needs a multicast lock or Android drops the queries. */
    private fun startMdns() {
        runCatching {
            multicastLock = (applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager)
                .createMulticastLock("lifehub-mdns").apply { setReferenceCounted(false); acquire() }
            mdns = MdnsResponder(hostName) { localInet() }.also { it.start() }
        }
    }

    private fun publish() {
        val s = server ?: return
        val ip = localAddress()
        val address = ip?.let { "http://$it:${s.port}" }
        val nameAddress = ip?.let { "http://$hostName.local:${s.port}" }
        _state.value = WebAccessState(running = true, address = address, nameAddress = nameAddress, code = s.auth.code)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(nameAddress ?: WebStr.noWifi.of(lang), s.auth.code))
    }

    override fun onDestroy() {
        scope.cancel()
        mdns?.stop()
        mdns = null
        runCatching { multicastLock?.release() }
        server?.stop()
        server = null
        _state.value = WebAccessState()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, WebStr.channel.of(lang), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(address: String, code: String): Notification {
        val stop = PendingIntent.getService(
            this, 1, Intent(this, WebAccessService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(WebStr.notifTitle.of(lang))
            .setContentText(WebStr.notifText.of(lang).format(address, code))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, WebStr.stop.of(lang), stop)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "web_access"
        private const val NOTIFICATION_ID = 7301
        private const val ACTION_STOP = "com.example.lifeorganizer.web.STOP"
        private const val ACTION_NEW_CODE = "com.example.lifeorganizer.web.NEW_CODE"
        private const val IDLE_LIMIT_MS = 30 * 60 * 1000L

        private val _state = MutableStateFlow(WebAccessState())
        val state: StateFlow<WebAccessState> = _state

        fun start(context: Context) {
            _state.value = _state.value.copy(failed = false)
            ContextCompat.startForegroundService(context, Intent(context, WebAccessService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WebAccessService::class.java))
        }

        fun newCode(context: Context) {
            context.startService(Intent(context, WebAccessService::class.java).setAction(ACTION_NEW_CODE))
        }

        /** IPv4 address in the local network, Wi-Fi preferred. */
        fun localInet(): Inet4Address? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedByDescending { it.name.startsWith("wlan") }
                .flatMap { nif -> nif.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress } }
                .firstOrNull()
        }.getOrNull()

        fun localAddress(): String? = localInet()?.hostAddress

        /** Restart with new name/port settings (only when running). */
        fun restart(context: Context) {
            if (!_state.value.running) return
            stop(context)
            start(context)
        }
    }
}
