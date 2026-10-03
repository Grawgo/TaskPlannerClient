package com.taskplanner.app

import com.taskplanner.app.data.remote.LanServerDiscovery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

class LanServerDiscoveryTest {
    @Test
    fun findsServerFromUdpResponse() {
        val loopback = InetAddress.getByName("127.0.0.1")
        DatagramSocket(0, loopback).use { server ->
            server.soTimeout = 2_000
            val worker = Thread {
                val request = DatagramPacket(ByteArray(64), 64)
                server.receive(request)
                assertEquals("TASK_PLANNER_DISCOVER_V1",
                    String(request.data, request.offset, request.length, StandardCharsets.US_ASCII))
                val response = "TASK_PLANNER_SERVER_V1:8080".toByteArray(StandardCharsets.US_ASCII)
                server.send(DatagramPacket(response, response.size, request.address, request.port))
            }.apply { start() }
            val found = LanServerDiscovery(server.localPort) { setOf(loopback) }.find()
            worker.join(2_000)
            assertEquals("127.0.0.1", found.host)
            assertEquals(8080, found.port)
        }
    }
}
