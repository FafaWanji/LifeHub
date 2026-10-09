package com.example.lifeorganizer.web

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

/**
 * Just enough multicast DNS to answer "who is <name>.local?" with the phone's IPv4 address, so the
 * browser can use http://lifehub.local:8080 instead of an IP that may change.
 */
object Mdns {
    const val TYPE_A = 1
    private const val TYPE_ANY = 255

    data class Question(val name: String, val type: Int, val unicastResponse: Boolean)

    fun parseQuestions(packet: ByteArray): List<Question> = runCatching {
        if (packet.size < 12) return emptyList()
        val count = u16(packet, 4)
        var pos = 12
        val out = mutableListOf<Question>()
        repeat(count) {
            val (name, next) = readName(packet, pos) ?: return out
            if (next + 4 > packet.size) return out
            out += Question(name.lowercase(), u16(packet, next), (packet[next + 2].toInt() and 0x80) != 0)
            pos = next + 4
        }
        out
    }.getOrDefault(emptyList())

    /** Does any question ask for the A record of [host].local? */
    fun wants(questions: List<Question>, host: String): Boolean =
        questions.any { it.name == "$host.local" && (it.type == TYPE_A || it.type == TYPE_ANY) }

    /**
     * Answer with one A record. [question] != null means a legacy unicast reply (query not from port 5353):
     * it echoes the question, keeps [id] and drops the cache-flush bit as RFC 6762 asks.
     */
    fun buildAnswer(id: Int, host: String, ip: ByteArray, question: Question?): ByteArray {
        val out = ByteArrayOutputStream()
        fun u16(v: Int) { out.write((v shr 8) and 0xFF); out.write(v and 0xFF) }
        fun name(n: String) { n.split('.').forEach { l -> out.write(l.length); out.write(l.toByteArray()) }; out.write(0) }
        u16(id); u16(0x8400); u16(if (question != null) 1 else 0); u16(1); u16(0); u16(0)
        if (question != null) { name(question.name); u16(question.type); u16(1) }
        name("$host.local")
        u16(TYPE_A)
        u16(if (question != null) 0x0001 else 0x8001)
        val ttl = if (question != null) 10 else 120
        u16(ttl shr 16); u16(ttl and 0xFFFF)
        u16(4)
        out.write(ip)
        return out.toByteArray()
    }

    private fun u16(b: ByteArray, at: Int) = ((b[at].toInt() and 0xFF) shl 8) or (b[at + 1].toInt() and 0xFF)

    /** Reads a (possibly compressed) name; returns it and the position after it in the original place. */
    private fun readName(b: ByteArray, start: Int): Pair<String, Int>? {
        val labels = mutableListOf<String>()
        var pos = start
        var end = -1
        var jumps = 0
        while (true) {
            if (pos >= b.size) return null
            val len = b[pos].toInt() and 0xFF
            when {
                len == 0 -> { if (end < 0) end = pos + 1; break }
                len and 0xC0 == 0xC0 -> {
                    if (pos + 1 >= b.size || ++jumps > 10) return null
                    if (end < 0) end = pos + 2
                    pos = ((len and 0x3F) shl 8) or (b[pos + 1].toInt() and 0xFF)
                }
                else -> {
                    if (pos + 1 + len > b.size) return null
                    labels += String(b, pos + 1, len)
                    pos += 1 + len
                }
            }
        }
        return labels.joinToString(".") to end
    }
}

/** Listens on 224.0.0.251:5353 and answers queries for [host].local. */
class MdnsResponder(private val host: String, private val address: () -> Inet4Address?) {
    private val group: InetAddress = InetAddress.getByName("224.0.0.251")
    @Volatile private var socket: MulticastSocket? = null

    fun start() {
        val ip = address() ?: return
        val s = MulticastSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(PORT))
            timeToLive = 255
        }
        val nif = NetworkInterface.getByInetAddress(ip)
        if (nif != null) {
            s.networkInterface = nif
            s.joinGroup(InetSocketAddress(group, PORT), nif)
        } else {
            @Suppress("DEPRECATION") s.joinGroup(group)
        }
        socket = s
        Thread({ loop(s) }, "lifehub-mdns").apply { isDaemon = true; start() }
    }

    fun stop() {
        socket?.close()
        socket = null
    }

    private fun loop(s: MulticastSocket) {
        // Tell the network right away, so caches pick the name up
        repeat(2) { announce(s); Thread.sleep(1000) }
        val buf = ByteArray(1500)
        while (!s.isClosed) {
            val p = DatagramPacket(buf, buf.size)
            try { s.receive(p) } catch (e: IOException) { break }
            val data = p.data.copyOf(p.length)
            if (data.size < 12 || (data[2].toInt() and 0x80) != 0) continue // a response, not a query
            val questions = Mdns.parseQuestions(data)
            if (!Mdns.wants(questions, host)) continue
            val ip = address()?.address ?: continue
            runCatching {
                if (p.port != PORT) {
                    val q = questions.first { it.name == "$host.local" }
                    val id = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                    send(s, Mdns.buildAnswer(id, host, ip, q), p.socketAddress)
                } else if (questions.any { it.unicastResponse && it.name == "$host.local" }) {
                    send(s, Mdns.buildAnswer(0, host, ip, null), p.socketAddress)
                } else {
                    send(s, Mdns.buildAnswer(0, host, ip, null), InetSocketAddress(group, PORT))
                }
            }
        }
    }

    private fun announce(s: MulticastSocket) {
        val ip = address()?.address ?: return
        runCatching { send(s, Mdns.buildAnswer(0, host, ip, null), InetSocketAddress(group, PORT)) }
    }

    private fun send(s: MulticastSocket, bytes: ByteArray, to: java.net.SocketAddress) {
        if (!s.isClosed) s.send(DatagramPacket(bytes, bytes.size, to))
    }

    companion object {
        private const val PORT = 5353
    }
}
