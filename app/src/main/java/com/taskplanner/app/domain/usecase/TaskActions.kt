package com.taskplanner.app.domain.usecase

import com.taskplanner.app.domain.model.*
import com.taskplanner.app.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class TaskActions @Inject constructor(private val repository: TaskRepository) {
    suspend fun list(query: TaskQuery = TaskQuery()) = repository.list(query)
    suspend fun listAll(query: TaskQuery = TaskQuery()): TaskPage {
        val tasks = linkedMapOf<String, Task>()
        var offset = query.offset
        do {
            currentCoroutineContext().ensureActive()
            val page = repository.list(query.copy(offset = offset))
            page.items.forEach { tasks[it.id] = it }
            offset += page.items.size
        } while (page.items.isNotEmpty() && offset < page.total)
        return TaskPage(tasks.values.toList(), tasks.size.toLong())
    }
    suspend fun get(id: String) = repository.get(id)
    suspend fun delete(id: String) = repository.delete(id)
    suspend fun save(id: String?, draft: TaskDraft): Task {
        val title = draft.title.trim()
        if (title.isEmpty() || title.length > 120) {
            throw TaskValidationException("Введите название от 1 до 120 символов")
        }
        if (draft.description.length > 2000) {
            throw TaskValidationException("Описание не должно превышать 2000 символов")
        }
        val normalized = draft.copy(title = title)
        return if (id == null) repository.create(normalized) else repository.update(id, normalized)
    }
    suspend fun setCompleted(task: Task, completed: Boolean): Task = repository.update(
        task.id,
        TaskDraft(task.title, task.description, task.dueAt, task.priority,
            if (completed) TaskStatus.DONE else TaskStatus.TODO)
    )
}
