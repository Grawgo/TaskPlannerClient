package com.taskplanner.app

import com.taskplanner.app.data.remote.TaskApi
import com.taskplanner.app.data.repository.RemoteTaskRepository
import com.taskplanner.app.domain.model.TaskQuery
import com.taskplanner.app.domain.model.TaskSort
import com.taskplanner.app.domain.repository.SessionProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class RemoteTaskRepositoryHttpTest {
    @Test fun sendsEncodedSearchAndBearerTokenToHttpApi() = runBlocking {
        val requests = CopyOnWriteArrayList<RecordedRequest>()
        withServer({ request ->
            requests += request
            200 to taskPageJson
        }) { baseUrl ->
            val repository = RemoteTaskRepository(taskApi(baseUrl), StaticSession("token-a"))
            val result = repository.list(TaskQuery(search = "100% готово", sort = TaskSort.DUE, ascending = true))

            assertEquals("100% готово", result.items.single().title)
            assertEquals("Bearer token-a", requests.single().authorization)
            val query = URLDecoder.decode(requests.single().rawQuery, StandardCharsets.UTF_8)
            assertTrue(query.contains("query=100% готово"))
            assertTrue(query.contains("sort=dueAt"))
            assertTrue(query.contains("order=asc"))
        }
    }

    @Test fun refreshesTokenOnceAfterUnauthorizedResponse() = runBlocking {
        val requests = CopyOnWriteArrayList<RecordedRequest>()
        val session = StaticSession("expired", "fresh")
        withServer({ request ->
            requests += request
            if (request.authorization == "Bearer expired") 401 to "" else 200 to taskPageJson
        }) { baseUrl ->
            val result = RemoteTaskRepository(taskApi(baseUrl), session).list(TaskQuery())
            assertEquals(1, result.items.size)
            assertEquals(listOf("Bearer expired", "Bearer fresh"), requests.map { it.authorization })
            assertEquals(1, session.refreshCount)
        }
    }

    private fun taskApi(baseUrl: String): TaskApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TaskApi::class.java)

    private suspend fun withServer(
        respond: (RecordedRequest) -> Pair<Int, String>,
        block: suspend (String) -> Unit,
    ) {
        val server = ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))
        val worker = thread(name = "task-api-test") {
            try {
                while (!server.isClosed) {
                    server.accept().use { socket ->
                        val input = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                        val requestLine = input.readLine() ?: error("Empty HTTP request")
                        var authorization: String? = null
                        while (true) {
                            val line = input.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Authorization:", ignoreCase = true)) {
                                authorization = line.substringAfter(':').trim()
                            }
                        }
                        val rawQuery = requestLine.substringAfter('?', "").substringBefore(' ')
                        val (status, body) = respond(RecordedRequest(rawQuery, authorization))
                        val bytes = body.toByteArray(StandardCharsets.UTF_8)
                        val head = "HTTP/1.1 $status Test\r\nContent-Type: application/json\r\n" +
                            "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        socket.getOutputStream().use { output ->
                            output.write(head.toByteArray(StandardCharsets.UTF_8))
                            output.write(bytes)
                        }
                    }
                }
            } catch (_: SocketException) {
                // Закрытие сокета завершает работу тестового сервера.
            }
        }
        try { block("http://127.0.0.1:${server.localPort}/") }
        finally { server.close(); worker.join(2_000) }
    }

    private data class RecordedRequest(val rawQuery: String, val authorization: String?)

    private class StaticSession(private val first: String, private val renewed: String = first) : SessionProvider {
        var refreshCount = 0
        override suspend fun idToken(forceRefresh: Boolean): String = if (forceRefresh) {
            refreshCount++
            renewed
        } else first
        override fun currentUid(): String = "test-user"
    }

    private val taskPageJson = """{
      "items":[{"id":"task-1","title":"100% готово","description":"",
        "dueAt":null,"priority":"MEDIUM","status":"TODO","completedAt":null,
        "createdAt":"2026-09-27T00:00:00Z","updatedAt":"2026-09-27T00:00:00Z"}],
      "total":1,"limit":50,"offset":0
    }"""
}
