package com.example.lifeorganizer.web

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
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

/** Why a running service keeps the server off for now (automatic modes only). */
enum class WaitReason { WIFI, KNOWN_WIFI, BATTERY }

data class WebAccessState(
    val running: Boolean = false,
    val address: String? = null,
    val nameAddress: String? = null,
    val code: String = "",
    val failed: Boolean = false,
    /** Service on, server paused – e.g. no (remembered) Wi-Fi or battery low. */
    val waiting: WaitReason? = null
)

/**
 * Keeps the local web server alive while PC access is on; shows address and code in a notification.
 * Started by hand or when the app opens it stops after 30 minutes without use. In the automatic modes
 * (Wi-Fi / always) it stays and only pauses the server while the conditions are not met.
 */
class WebAccessService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: WebServer? = null
    private var mdns: MdnsResponder? = null
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null
    private var hostName = WebSettings.DEFAULT_NAME
    private var lang = "en"
    private var loopStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // "End" in the notification: really off, no automatic restart
            WebSettings(this).stoppedByUser = true
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_NEW_CODE) {
            server?.auth?.newCode()
            publish()
            return START_NOT_STICKY
        }
        val settings = WebSettings(this)
        // Restarted by the system (sticky) after it had been ended by hand: stay off
        if (intent == null && settings.stoppedByUser) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!loopStarted) {
            lang = runBlocking { SettingsManager(this@WebAccessService).languageCode.first() }
            createChannel()
            startInForeground(buildNotification(WebStr.notifTitle.of(lang), "…"))
            loopStarted = true
            evaluate()
            scope.launch {
                while (isActive) {
                    delay(3_000)
                    if (!evaluate()) break
                }
            }
        }
        // Killed by the system in an automatic mode: Android starts it again
        return if (settings.autoStart.persistent) START_STICKY else START_NOT_STICKY
    }

    /** Starts, pauses or stops the server as the settings say. Returns false once the service is stopping. */
    @Synchronized
    private fun evaluate(): Boolean {
        val settings = WebSettings(this)
        val wait = waitReason(settings)
        val s = server
        if (!settings.autoStart.persistent) {
            // By hand / on app open: no pausing, but low battery and 30 minutes without use end it
            if (wait == WaitReason.BATTERY) { stopSelf(); return false }
            if (s == null) {
                if (!startServer(settings)) return false
            } else if (System.currentTimeMillis() - s.lastActivity > IDLE_LIMIT_MS) {
                stopSelf(); return false
            }
        } else if (wait != null) {
            if (s != null) stopServer()
            if (_state.value.waiting != wait || _state.value.running) publishWaiting(wait)
            return true
        } else if (s == null) {
            if (!startServer(settings)) return false
        }
        // The code changes after a pairing: keep notification and screen up to date
        server?.let { if (it.auth.code != _state.value.code || localAddress() != _state.value.address) publish() }
        return true
    }

    private fun waitReason(settings: WebSettings): WaitReason? = when {
        settings.lowBatteryOff > 0 && batteryLow(settings.lowBatteryOff) -> WaitReason.BATTERY
        settings.autoStart == AutoStart.WIFI && !NetworkId.onWifi(this) -> WaitReason.WIFI
        settings.autoStart == AutoStart.WIFI && settings.onlyKnownWifi &&
            !NetworkId.matches(NetworkId.currentIds(this), settings.knownNetworks) -> WaitReason.KNOWN_WIFI
        else -> null
    }

    /** Below [percent] and not charging. */
    private fun batteryLow(percent: Int): Boolean {
        val status = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val plugged = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (plugged || level < 0 || scale <= 0) return false
        return level * 100 / scale < percent
    }

    private fun startServer(settings: WebSettings): Boolean {
        hostName = settings.name
        val started = runCatching { WebServer(this, settings.port).also { it.start() } }
        server = started.getOrNull()
        if (server == null) {
            _state.value = WebAccessState(failed = true)
            stopSelf()
            return false
        }
        startMdns()
        publish()
        return true
    }

    private fun stopServer() {
        mdns?.stop()
        mdns = null
        runCatching { multicastLock?.release() }
        multicastLock = null
        server?.stop()
        server = null
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
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            buildNotification(WebStr.notifTitle.of(lang), WebStr.notifText.of(lang).format(nameAddress ?: WebStr.noWifi.of(lang), s.auth.code))
        )
    }

    private fun publishWaiting(reason: WaitReason) {
        _state.value = WebAccessState(running = false, waiting = reason)
        val text = when (reason) {
            WaitReason.WIFI -> WebStr.waitWifi
            WaitReason.KNOWN_WIFI -> WebStr.waitKnownWifi
            WaitReason.BATTERY -> WebStr.waitBattery
        }.of(lang)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(WebStr.notifWaiting.of(lang), text))
    }

    override fun onDestroy() {
        scope.cancel()
        stopServer()
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

    private fun buildNotification(title: String, text: String): Notification {
        val stop = PendingIntent.getService(
            this, 1, Intent(this, WebAccessService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(text)
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
        /** A browser counts as connected while it asked within this time (open tabs ask every few seconds). */
        const val ONLINE_MS = 15_000L

        private val _state = MutableStateFlow(WebAccessState())
        val state: StateFlow<WebAccessState> = _state

        private val _lastSeen = MutableStateFlow<Map<String, Long>>(emptyMap())
        /** Paired browser id → time of its last request in this run. */
        val lastSeen: StateFlow<Map<String, Long>> = _lastSeen

        internal fun markSeen(browserId: String) {
            _lastSeen.value = _lastSeen.value + (browserId to System.currentTimeMillis())
        }

        /** True when the service is up – serving or waiting for its conditions. */
        val active: Boolean get() = _state.value.running || _state.value.waiting != null

        /** Switched on by hand: automatic starts are allowed again. */
        fun start(context: Context) {
            WebSettings(context).stoppedByUser = false
            startService(context)
        }

        private fun startService(context: Context) {
            _state.value = _state.value.copy(failed = false)
            ContextCompat.startForegroundService(context, Intent(context, WebAccessService::class.java))
        }

        /** Switched off by hand: stays off until switched on again (no automatic start). */
        fun stop(context: Context) {
            WebSettings(context).stoppedByUser = true
            context.stopService(Intent(context, WebAccessService::class.java))
        }

        /** Automatic start (app open, reboot, mode change); nothing after an end by hand. */
        internal fun autoStart(context: Context) {
            if (!WebSettings(context).stoppedByUser) startService(context)
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
            if (!active) return
            context.stopService(Intent(context, WebAccessService::class.java))
            startService(context)
        }
    }
}

/** Starts PC access by itself as chosen in its settings: when the app opens, after a reboot, after an update. */
object WebAutoStart {
    fun onAppOpen(context: Context) {
        val settings = WebSettings(context)
        if (WebAccessService.active) return
        when (settings.autoStart) {
            AutoStart.OFF -> Unit
            // Only worth it in a Wi-Fi; stops again after 30 minutes without use
            AutoStart.APP_OPEN -> if (NetworkId.onWifi(context)) WebAccessService.autoStart(context)
            AutoStart.WIFI, AutoStart.ALWAYS -> WebAccessService.autoStart(context)
        }
    }

    /** After changing the mode in the settings: the automatic modes start right away (a new choice lifts an end by hand). */
    fun apply(context: Context) {
        WebSettings(context).stoppedByUser = false
        if (WebSettings(context).autoStart.persistent && !WebAccessService.active) WebAccessService.autoStart(context)
    }
}

class WebBootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (WebSettings(context).autoStart.persistent) runCatching { WebAccessService.autoStart(context) }
    }
}
