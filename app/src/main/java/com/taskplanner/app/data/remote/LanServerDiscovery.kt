package com.taskplanner.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

private const val DISCOVERY_PORT = 45678
private const val REQUEST = "TASK_PLANNER_DISCOVER_V1"
private const val RESPONSE_PREFIX = "TASK_PLANNER_SERVER_V1:"

class LanServerNotFoundException(message: String) : IOException(message)

/** Обнаружение Ktor-сервера по широковещательному запросу в локальной сети. */
class LanServerDiscovery(
    private val discoveryPort: Int = DISCOVERY_PORT,
    private val targetsProvider: () -> Set<InetAddress> = ::broadcastTargets,
) {
    fun find(): LanServer {
        val payload = REQUEST.toByteArray(StandardCharsets.US_ASCII)
        DatagramSocket().use { socket ->
            socket.broadcast = true
            var sent = false
            for (address in targetsProvider()) {
                try {
                    socket.send(DatagramPacket(payload, payload.size, address, discoveryPort))
                    sent = true
                } catch (_: IOException) {
                    // Ошибка одного интерфейса не прерывает отправку через остальные.
                }
            }
            if (!sent) throw LanServerNotFoundException("Не удалось выполнить поиск сервера в локальной сети")

            val deadline = System.nanoTime() + 1_500_000_000L
            while (true) {
                val remaining = ((deadline - System.nanoTime()) / 1_000_000L).toInt()
                if (remaining <= 0) break
                socket.soTimeout = remaining
                val packet = DatagramPacket(ByteArray(64), 64)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    break
                }
                val response = String(packet.data, packet.offset, packet.length, StandardCharsets.US_ASCII)
                if (!response.startsWith(RESPONSE_PREFIX)) continue
                val port = response.removePrefix(RESPONSE_PREFIX).toIntOrNull()
                if (port != null && port in 1..65535) {
                    return LanServer(packet.address.hostAddress ?: continue, port)
                }
            }
        }
        throw LanServerNotFoundException("Сервер не найден. Подключите телефон и ноутбук к одной сети Wi-Fi")
    }
}

private fun broadcastTargets(): Set<InetAddress> {
    val targets = linkedSetOf<InetAddress>()
    runCatching {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val network = interfaces.nextElement()
            if (!network.isUp || network.isLoopback) continue
            network.interfaceAddresses.mapNotNullTo(targets) { it.broadcast }
        }
    }
    targets += InetAddress.getByName("255.255.255.255")
    return targets
}

data class LanServer(val host: String, val port: Int)

class LanApiInterceptor(private val discovery: LanServerDiscovery) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val server = discovery.find()
        val request = chain.request()
        val url = request.url.newBuilder().scheme("http").host(server.host).port(server.port).build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
