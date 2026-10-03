package com.taskplanner.app.data.repository

import com.taskplanner.app.data.remote.TaskApi
import com.taskplanner.app.data.remote.TaskDto
import com.taskplanner.app.data.remote.TaskInputDto
import com.taskplanner.app.data.remote.LanServerNotFoundException
import com.taskplanner.app.domain.model.*
import com.taskplanner.app.domain.repository.SessionProvider
import com.taskplanner.app.domain.repository.TaskRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class RemoteTaskRepository @Inject constructor(
    private val api: TaskApi,
    private val session: SessionProvider,
) : TaskRepository {
    override suspend fun list(query: TaskQuery): TaskPage = authorized { token ->
        val dto = api.list(
            token, query.search?.takeIf { it.isNotBlank() },
            query.status.name.takeUnless { query.status == TaskStatusFilter.ALL },
            query.priority?.name,
            if (query.sort == TaskSort.DUE) "dueAt" else "updatedAt",
            if (query.ascending) "asc" else "desc",
            query.limit, query.offset,
        )
        TaskPage(dto.items.map(TaskDto::toDomain), dto.total)
    }

    override suspend fun get(id: String): Task = authorized { api.get(it, id).toDomain() }
    override suspend fun create(draft: TaskDraft): Task = authorized { api.create(it, draft.toDto()).toDomain() }
    override suspend fun update(id: String, draft: TaskDraft): Task =
        authorized { api.update(it, id, draft.toDto()).toDomain() }
    override suspend fun delete(id: String) {
        authorized { token ->
            val response = api.delete(token, id)
            if (!response.isSuccessful) throw HttpException(response)
        }
    }

    private suspend fun <T> authorized(block: suspend (String) -> T): T {
        val firstToken = session.idToken() ?: throw AuthenticationRequiredException()
        try {
            return block("Bearer $firstToken")
        } catch (error: HttpException) {
            if (error.code() == 401) {
                val fresh = session.idToken(forceRefresh = true) ?: throw AuthenticationRequiredException()
                try { return block("Bearer $fresh") }
                catch (retry: HttpException) {
                    if (retry.code() == 401) throw AuthenticationRequiredException()
                    throw retry.toDomainError()
                }
                catch (error: IOException) {
                    throw error.toConnectionError()
                }
            }
            throw error.toDomainError()
        } catch (error: IOException) {
            throw error.toConnectionError()
        }
    }
}

private fun IOException.toConnectionError(): TaskRemoteException =
    TaskRemoteException(if (this is LanServerNotFoundException) message.orEmpty()
    else "Нет соединения с сервером. Проверьте сеть и повторите")

private fun HttpException.toDomainError(): TaskRemoteException = when (code()) {
    404 -> TaskRemoteException("Задача не найдена")
    503 -> TaskRemoteException("Сервер временно недоступен")
    else -> TaskRemoteException("Не удалось выполнить запрос (${code()})")
}

private fun TaskDto.toDomain() = Task(
    id, title, description, dueAt, priority, status, completedAt, createdAt, updatedAt
)

private fun TaskDraft.toDto() = TaskInputDto(title, description, dueAt, priority, status)
