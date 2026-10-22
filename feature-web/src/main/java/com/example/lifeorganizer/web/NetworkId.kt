package com.example.lifeorganizer.web

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address

/**
 * Recognises a Wi-Fi again. The network name (SSID) is only readable with location access, so a network is
 * also remembered by its router address and size ("gw:192.168.178.1/24"); either one matching counts.
 */
object NetworkId {
    fun onWifi(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION")
        cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
    }.getOrDefault(false)

    /** The current Wi-Fi's name, null if not readable (no location access) or not in a Wi-Fi. */
    fun ssid(context: Context): String? = runCatching {
        @Suppress("DEPRECATION")
        val raw = context.applicationContext.getSystemService(WifiManager::class.java).connectionInfo?.ssid
        raw?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != "<unknown ssid>" && it != WifiManager.UNKNOWN_SSID }
    }.getOrNull()

    /** Ids of the Wi-Fi the phone is in: "ssid:<name>" when readable and "gw:<router>/<prefix>". */
    fun currentIds(context: Context): Set<String> = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION")
        val wifi = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
            ?: return@runCatching emptySet()
        val lp = cm.getLinkProperties(wifi)
        val gateway = lp?.routes?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress
        val prefix = lp?.linkAddresses?.firstOrNull { it.address is Inet4Address }?.prefixLength
        buildSet {
            ssid(context)?.let { add("ssid:$it") }
            if (gateway != null) add("gw:$gateway/${prefix ?: 0}")
        }
    }.getOrDefault(emptySet())

    /** A readable name for the current Wi-Fi: its SSID, otherwise the router address. */
    fun label(context: Context): String? =
        ssid(context) ?: currentIds(context).firstOrNull { it.startsWith("gw:") }?.removePrefix("gw:")

    fun matches(current: Set<String>, known: Map<String, Set<String>>): Boolean =
        current.isNotEmpty() && known.values.any { ids -> ids.any { it in current } }
}
