package com.taskplanner.app

import com.taskplanner.app.domain.model.*
import com.taskplanner.app.domain.repository.TaskRepository
import com.taskplanner.app.domain.usecase.TaskActions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskActionsTest {
    private val repository = RecordingRepository()
    private val actions = TaskActions(repository)

    @Test fun trimsTitleBeforeSaving() = runBlocking {
        actions.save(null, TaskDraft("  Доклад  ", "", null, TaskPriority.MEDIUM, TaskStatus.TODO))
        assertEquals("Доклад", repository.saved?.title)
    }

    @Test fun rejectsBlankTitle() {
        assertThrows(TaskValidationException::class.java) {
            runBlocking { actions.save(null, TaskDraft("   ", "", null, TaskPriority.MEDIUM, TaskStatus.TODO)) }
        }
    }

    @Test fun rejectsTooLongDescription() {
        assertThrows(TaskValidationException::class.java) {
            runBlocking { actions.save(null, TaskDraft("Задача", "x".repeat(2001), null, TaskPriority.MEDIUM, TaskStatus.TODO)) }
        }
    }

    @Test fun readsTasksBeyondFirstServerPage() = runBlocking {
        val draft = TaskDraft("Задача", "", null, TaskPriority.MEDIUM, TaskStatus.TODO)
        val prototype = repository.create(draft)
        repository.items = (1..73).map { prototype.copy(id = it.toString()) }
        val result = actions.listAll(TaskQuery(search = "Задача", priority = TaskPriority.MEDIUM))
        assertEquals(73, result.items.size)
        assertEquals(listOf(0, 50), repository.queries.map { it.offset })
        assertEquals(listOf("Задача", "Задача"), repository.queries.map { it.search })
        assertEquals(listOf(TaskPriority.MEDIUM, TaskPriority.MEDIUM), repository.queries.map { it.priority })
    }

    private class RecordingRepository : TaskRepository {
        var saved: TaskDraft? = null
        var items: List<Task> = emptyList()
        val queries = mutableListOf<TaskQuery>()
        override suspend fun list(query: TaskQuery): TaskPage {
            queries += query
            return TaskPage(items.drop(query.offset).take(query.limit), items.size.toLong())
        }
        override suspend fun get(id: String): Task = error("unused")
        override suspend fun create(draft: TaskDraft): Task { saved = draft; return task(draft) }
        override suspend fun update(id: String, draft: TaskDraft): Task { saved = draft; return task(draft) }
        override suspend fun delete(id: String) = Unit
        private fun task(draft: TaskDraft) = Task(
            "id", draft.title, draft.description, draft.dueAt, draft.priority, draft.status,
            null, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z"
        )
    }
}
