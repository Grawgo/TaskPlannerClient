package com.taskplanner.app.domain.model

enum class TaskPriority { LOW, MEDIUM, HIGH }
enum class TaskStatus { TODO, IN_PROGRESS, DONE }
enum class TaskStatusFilter { ALL, ACTIVE, DONE }
enum class TaskSort { UPDATED, DUE }

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueAt: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val completedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class TaskDraft(
    val title: String,
    val description: String,
    val dueAt: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
)

data class TaskPage(val items: List<Task>, val total: Long)

data class TaskQuery(
    val search: String? = null,
    val status: TaskStatusFilter = TaskStatusFilter.ALL,
    val priority: TaskPriority? = null,
    val sort: TaskSort = TaskSort.UPDATED,
    val ascending: Boolean = false,
    val limit: Int = 50,
    val offset: Int = 0,
)

class AuthenticationRequiredException : Exception("Для работы с задачами войдите в аккаунт")
class TaskValidationException(message: String) : Exception(message)
class TaskRemoteException(message: String) : Exception(message)
