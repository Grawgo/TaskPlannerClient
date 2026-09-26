package com.taskplanner.app.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskplanner.app.domain.model.*
import com.taskplanner.app.domain.repository.SearchHistoryRepository
import com.taskplanner.app.domain.repository.AuthRepository
import com.taskplanner.app.domain.repository.ThemeRepository
import com.taskplanner.app.domain.usecase.TaskActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val loading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val requiresLogin: Boolean = false,
    val filter: TaskStatusFilter = TaskStatusFilter.ALL,
    val sort: TaskSort = TaskSort.UPDATED,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val actions: TaskActions,
    auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()
    private var refreshSequence = 0
    init {
        viewModelScope.launch {
            auth.authState.collect {
                _state.value = _state.value.copy(tasks = emptyList())
                refresh()
            }
        }
    }

    fun refresh() {
        val sequence = ++refreshSequence
        val current = _state.value
        viewModelScope.launch {
            _state.value = current.copy(loading = true, error = null, requiresLogin = false)
            try {
                val result = actions.list(TaskQuery(status = current.filter, sort = current.sort))
                if (sequence == refreshSequence) _state.value = _state.value.copy(loading = false, tasks = result.items)
            } catch (_: AuthenticationRequiredException) {
                if (sequence == refreshSequence) _state.value = _state.value.copy(loading = false, tasks = emptyList(), requiresLogin = true)
            } catch (error: Exception) {
                if (sequence == refreshSequence) _state.value = _state.value.copy(loading = false, error = error.userMessage())
            }
        }
    }

    fun setFilter(filter: TaskStatusFilter) { _state.value = _state.value.copy(filter = filter); refresh() }
    fun setSort(sort: TaskSort) { _state.value = _state.value.copy(sort = sort); refresh() }
    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            try { actions.setCompleted(task, task.status != TaskStatus.DONE); refresh() }
            catch (error: Exception) { _state.value = _state.value.copy(error = error.userMessage()) }
        }
    }
}

data class DetailUiState(val loading: Boolean = true, val task: Task? = null, val error: String? = null)

@HiltViewModel
class DetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val actions: TaskActions,
) : ViewModel() {
    val id: String = checkNotNull(handle["id"])
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try { _state.value = DetailUiState(task = actions.get(id), loading = false) }
            catch (error: Exception) { _state.value = DetailUiState(error = error.userMessage(), loading = false) }
        }
    }
    fun toggleCompleted() {
        val task = _state.value.task ?: return
        viewModelScope.launch {
            try { _state.value = _state.value.copy(task = actions.setCompleted(task, task.status != TaskStatus.DONE)) }
            catch (error: Exception) { _state.value = _state.value.copy(error = error.userMessage()) }
        }
    }
    suspend fun delete(): Boolean = try { actions.delete(id); true }
    catch (error: Exception) { _state.value = _state.value.copy(error = error.userMessage()); false }
}

data class EditorUiState(
    val loading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val dueAt: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val actions: TaskActions,
) : ViewModel() {
    private val id: String? = handle.get<String>("taskId")?.takeIf { it.isNotBlank() }
    val editing: Boolean get() = id != null
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    init {
        if (id != null) viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val task = actions.get(id)
                _state.value = EditorUiState(
                    title = task.title, description = task.description, dueAt = task.dueAt,
                    priority = task.priority, status = task.status,
                )
            } catch (error: Exception) { _state.value = _state.value.copy(loading = false, error = error.userMessage()) }
        }
    }
    fun title(value: String) { _state.value = _state.value.copy(title = value, error = null) }
    fun description(value: String) { _state.value = _state.value.copy(description = value, error = null) }
    fun dueAt(value: String?) { _state.value = _state.value.copy(dueAt = value) }
    fun priority(value: TaskPriority) { _state.value = _state.value.copy(priority = value) }
    fun status(value: TaskStatus) { _state.value = _state.value.copy(status = value) }
    suspend fun save(): Boolean {
        val current = _state.value
        if (current.saving) return false
        _state.value = current.copy(saving = true, error = null)
        return try {
            actions.save(id, TaskDraft(current.title, current.description, current.dueAt, current.priority, current.status))
            _state.value = _state.value.copy(saving = false)
            true
        } catch (error: Exception) {
            _state.value = _state.value.copy(saving = false, error = error.userMessage())
            false
        }
    }
}

data class SearchUiState(
    val loading: Boolean = false,
    val results: List<Task> = emptyList(),
    val searched: Boolean = false,
    val error: String? = null,
    val requiresLogin: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val actions: TaskActions,
    private val historyRepository: SearchHistoryRepository,
    auth: AuthRepository,
) : ViewModel() {
    val query: StateFlow<String> = handle.getStateFlow("query", "")
    val history = historyRepository.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var resultQuery: String? = null
    private var requestVersion = 0
    init {
        viewModelScope.launch {
            auth.authState.drop(1).collect {
                requestVersion++
                searchJob?.cancel()
                resultQuery = null
                handle["query"] = ""
                _state.value = SearchUiState()
            }
        }
    }
    fun setQuery(value: String) {
        val updated = value.take(120)
        if (updated == query.value) return
        requestVersion++
        searchJob?.cancel()
        resultQuery = null
        handle["query"] = updated
        _state.value = SearchUiState()
    }
    fun search() {
        val text = query.value.trim()
        if (text.isEmpty()) { _state.value = SearchUiState(); return }
        val version = ++requestVersion
        searchJob?.cancel()
        resultQuery = null
        searchJob = viewModelScope.launch {
            _state.value = SearchUiState(loading = true, searched = true)
            try {
                val results = actions.list(TaskQuery(search = text)).items
                if (version != requestVersion) return@launch
                resultQuery = text
                _state.value = SearchUiState(results = results, searched = true)
            } catch (_: AuthenticationRequiredException) {
                if (version == requestVersion) _state.value = SearchUiState(searched = true, requiresLogin = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (version == requestVersion) _state.value = SearchUiState(searched = true, error = error.userMessage())
            }
        }
    }
    fun openResult() {
        val completedQuery = resultQuery ?: return
        viewModelScope.launch { historyRepository.remember(completedQuery) }
    }
    fun clearHistory() { viewModelScope.launch { historyRepository.clear() } }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val theme: ThemeRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val darkMode = theme.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val user = auth.authState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun setDarkMode(enabled: Boolean) { viewModelScope.launch { theme.setDarkMode(enabled) } }
    fun signOut() { auth.signOut() }
}

private fun Exception.userMessage(): String = when (this) {
    is AuthenticationRequiredException, is TaskValidationException, is TaskRemoteException,
    is AuthOperationException -> message ?: "Неизвестная ошибка"
    else -> "Не удалось выполнить действие. Повторите попытку"
}
