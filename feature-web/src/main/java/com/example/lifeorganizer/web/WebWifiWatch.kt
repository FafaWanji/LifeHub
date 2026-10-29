package com.example.lifeorganizer.web

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build

/**
 * While PC access is paused in an automatic mode (no Wi-Fi, a strange one, battery low) the service is off.
 * This network watch wakes the app whenever a network comes up, so it can start again – also when the app
 * itself is not running.
 */
object WebWifiWatch {
    internal const val ACTION_NETWORK = "com.example.lifeorganizer.web.NETWORK"
    internal const val ACTION_START = "com.example.lifeorganizer.web.START"

    // The network callback needs a mutable PendingIntent (the system adds the network to it); the alarm does not
    private fun pending(context: Context, action: String) = PendingIntent.getBroadcast(
        context, if (action == ACTION_NETWORK) 11 else 12,
        Intent(context, WebWifiReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or if (action == ACTION_NETWORK) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
    )

    /** Calls back on every Wi-Fi that comes up; the same PendingIntent replaces an earlier watch. */
    fun arm(context: Context) {
        runCatching {
            val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            context.getSystemService(ConnectivityManager::class.java).registerNetworkCallback(request, pending(context, ACTION_NETWORK))
        }
    }

    fun disarm(context: Context) {
        runCatching { context.getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(pending(context, ACTION_NETWORK)) }
    }

    /**
     * Android may refuse to start a foreground service from the background; a moment later from an exact
     * alarm it is allowed.
     */
    internal fun startSoon(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1_000, pending(context, ACTION_START))
    }
}

/** A Wi-Fi came up (or the follow-up alarm fired): start PC access if its mode wants it and it can serve now. */
class WebWifiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = WebSettings(context)
        if (!settings.autoStart.persistent || settings.stoppedByUser) { WebWifiWatch.disarm(context); return }
        if (WebAccessService.active) return
        runCatching { WebAccessService.autoStart(context) }.onFailure {
            if (intent.action == WebWifiWatch.ACTION_NETWORK) WebWifiWatch.startSoon(context)
        }
    }
}
