package com.taskplanner.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.taskplanner.app.domain.model.AuthUser
import com.taskplanner.app.domain.model.Task
import com.taskplanner.app.domain.model.TaskDraft
import com.taskplanner.app.domain.model.TaskPage
import com.taskplanner.app.domain.model.TaskPriority
import com.taskplanner.app.domain.model.TaskQuery
import com.taskplanner.app.domain.model.TaskRemoteException
import com.taskplanner.app.domain.model.TaskStatus
import com.taskplanner.app.domain.repository.AuthRepository
import com.taskplanner.app.domain.repository.SearchHistoryRepository
import com.taskplanner.app.domain.repository.TaskRepository
import com.taskplanner.app.domain.usecase.TaskActions
import com.taskplanner.app.presentation.SearchViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test fun editingQueryDiscardsLateResultsAndPreservesTextInSavedState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val handle = SavedStateHandle()
            val tasks = QueuedTasks()
            val first = CompletableDeferred<TaskPage>()
            val second = CompletableDeferred<TaskPage>()
            tasks.responses.addAll(listOf(first, second))
            val auth = FakeAuth()
            val vm = SearchViewModel(handle, TaskActions(tasks), MemoryHistory(), auth)
            store.put("search", vm)
            runCurrent()

            vm.setQuery("первый")
            vm.search()
            runCurrent()
            assertTrue(vm.state.value.loading)

            vm.setQuery("второй")
            assertFalse(vm.state.value.searched)
            vm.search()
            runCurrent()
            first.complete(TaskPage(listOf(task("старый")), 1))
            runCurrent()
            assertTrue(vm.state.value.loading)

            second.complete(TaskPage(listOf(task("новый")), 1))
            runCurrent()
            assertEquals("новый", vm.state.value.results.single().title)
            assertEquals("второй", handle.get<String>("query"))
            assertEquals(listOf("первый", "второй"), tasks.queries)

            auth.signOut()
            runCurrent()
            assertEquals("", vm.query.value)
            assertTrue(vm.state.value.results.isEmpty())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun retryUsesSameQueryAndHistoryIsSavedOnlyAfterOpeningResult() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val tasks = QueuedTasks()
            val failed = CompletableDeferred<TaskPage>()
            val successful = CompletableDeferred<TaskPage>()
            tasks.responses.addAll(listOf(failed, successful))
            val history = MemoryHistory()
            val vm = SearchViewModel(SavedStateHandle(), TaskActions(tasks), history, FakeAuth())
            store.put("search", vm)
            runCurrent()

            vm.setQuery("  отчёт  ")
            vm.search()
            runCurrent()
            failed.completeExceptionally(TaskRemoteException("Нет соединения"))
            runCurrent()
            assertEquals("Нет соединения", vm.state.value.error)
            assertTrue(history.saved.isEmpty())

            vm.search()
            runCurrent()
            successful.complete(TaskPage(listOf(task("Отчёт")), 1))
            runCurrent()
            assertEquals(listOf("отчёт", "отчёт"), tasks.queries)
            assertTrue(history.saved.isEmpty())

            vm.openResult()
            runCurrent()
            assertEquals(listOf("отчёт"), history.saved)
            vm.clearHistory()
            runCurrent()
            assertTrue(history.saved.isEmpty())
            assertTrue(history.observe().first().isEmpty())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private fun task(title: String) = Task(
        id = title, title = title, description = "", dueAt = null,
        priority = TaskPriority.MEDIUM, status = TaskStatus.TODO,
        completedAt = null, createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )
}

private class QueuedTasks : TaskRepository {
    val responses = mutableListOf<CompletableDeferred<TaskPage>>()
    val queries = mutableListOf<String>()
    override suspend fun list(query: TaskQuery): TaskPage {
        queries += query.search.orEmpty()
        return withContext(NonCancellable) { responses.removeAt(0).await() }
    }
    override suspend fun get(id: String): Task = error("unused")
    override suspend fun create(draft: TaskDraft): Task = error("unused")
    override suspend fun update(id: String, draft: TaskDraft): Task = error("unused")
    override suspend fun delete(id: String) = error("unused")
}

private class MemoryHistory : SearchHistoryRepository {
    val saved = mutableListOf<String>()
    private val flow = MutableStateFlow<List<String>>(emptyList())
    override fun observe() = flow
    override suspend fun remember(query: String) { saved += query; flow.value = saved.toList() }
    override suspend fun clear() { saved.clear(); flow.value = emptyList() }
}

private class FakeAuth : AuthRepository {
    private val user = MutableStateFlow<AuthUser?>(AuthUser("test-user", "user@example.com"))
    override val authState = user
    override val isConfigured = true
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun register(email: String, password: String) = Unit
    override suspend fun sendPasswordReset(email: String) = Unit
    override fun signOut() { user.value = null }
}
