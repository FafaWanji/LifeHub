package com.example.lifeorganizer.web

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

/** Asks the network for <name>.local like a PC does and expects the phone's address back. */
@RunWith(AndroidJUnit4::class)
class MdnsResponderTest {
    @Test fun answersQueryForOwnName() {
        val ip = WebAccessService.localInet()
        assertNotNull("device needs a network address", ip)
        val responder = MdnsResponder("lifehubtest") { ip }
        responder.start()
        try {
            val query = ByteArrayOutputStream().apply {
                write(byteArrayOf(0x12, 0x34, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0))
                "lifehubtest.local".split('.').forEach { write(it.length); write(it.toByteArray()) }
                write(byteArrayOf(0, 0, 1, 0, 1))
            }.toByteArray()
            // A socket on a random port gets a direct (legacy unicast) answer
            MulticastSocket().use { s ->
                s.soTimeout = 3000
                s.networkInterface = java.net.NetworkInterface.getByInetAddress(ip)
                s.send(DatagramPacket(query, query.size, InetAddress.getByName("224.0.0.251"), 5353))
                val buf = ByteArray(1500)
                val reply = DatagramPacket(buf, buf.size)
                s.receive(reply)
                val data = reply.data.copyOf(reply.length)
                assertArrayEquals(byteArrayOf(0x12, 0x34), data.copyOfRange(0, 2))
                assertArrayEquals(ip!!.address, data.copyOfRange(data.size - 4, data.size))
            }
        } finally {
            responder.stop()
        }
    }
}
