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

    private class RecordingRepository : TaskRepository {
        var saved: TaskDraft? = null
        override suspend fun list(query: TaskQuery) = TaskPage(emptyList(), 0)
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
