package com.taskplanner.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.taskplanner.app.domain.model.TaskSort
import com.taskplanner.app.domain.model.TaskStatus
import com.taskplanner.app.domain.model.TaskStatusFilter
import com.taskplanner.app.domain.model.TaskPriority
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onSearch: () -> Unit,
    onLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)) {
            item {
                PlannerHeader("Мои задачи", "Все ваши планы под рукой")
                OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Найти задачу")
                }
                FlowRow(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TaskStatusFilter.ALL to "Все",
                        TaskStatusFilter.ACTIVE to "Активные",
                        TaskStatusFilter.DONE to "Готово",
                    ).forEach { (filter, label) ->
                        FilterChip(selected = state.filter == filter, onClick = { viewModel.setFilter(filter) }, label = { Text(label) })
                    }
                }
                FlowRow(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.priority == null, onClick = { viewModel.setPriority(null) }, label = { Text("Любой приоритет") })
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(selected = state.priority == priority, onClick = { viewModel.setPriority(priority) },
                            label = { Text(priority.label().removeSuffix(" приоритет")) })
                    }
                }
                FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Задачи", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { viewModel.setSort(if (state.sort == TaskSort.UPDATED) TaskSort.DUE else TaskSort.UPDATED) }) {
                        Text(if (state.sort == TaskSort.UPDATED) "По изменению" else "По сроку")
                    }
                }
            }
            when {
                state.loading -> item { Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                state.requiresLogin -> item { CenterMessage("Войдите в аккаунт", "После входа здесь появятся ваши задачи", "Войти", onLogin) }
                state.error != null -> item { CenterMessage("Не удалось загрузить задачи", state.error!!, "Повторить", viewModel::refresh) }
                state.tasks.isEmpty() -> item { CenterMessage("Пока нет задач", "Создайте первую задачу, чтобы начать планирование", "Создать задачу", onCreate) }
                else -> items(state.tasks, key = { it.id }) { task ->
                    TaskCard(task, onOpen = { onOpen(task.id) }, onToggle = { viewModel.toggleCompleted(task) })
                }
            }
        }
        FloatingActionButton(onClick = onCreate, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Создать задачу")
        }
    }
}

@Composable
fun DetailScreen(viewModel: DetailViewModel, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        PlannerHeader("Задача", onBack = onBack)
        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.task == null -> CenterMessage("Не удалось открыть задачу", state.error ?: "Задача не найдена", "Обновить", viewModel::refresh)
            else -> {
                val task = state.task!!
                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(task.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(task.status.label(), color = MaterialTheme.colorScheme.primary)
                    Text("Описание", style = MaterialTheme.typography.titleMedium)
                    Text(task.description.ifBlank { "Описание не добавлено" })
                    Text("Приоритет: ${task.priority.label()}")
                    Text("Срок: ${task.dueAt?.let(::formatDueAt) ?: "Не указан"}")
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = viewModel::toggleCompleted, modifier = Modifier.fillMaxWidth()) {
                        Text(if (task.status == TaskStatus.DONE) "Вернуть в работу" else "Отметить выполненной")
                    }
                    OutlinedButton(onClick = { onEdit(task.id) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Text("  Изменить")
                    }
                    OutlinedButton(onClick = { scope.launch { if (viewModel.delete()) onBack() } }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Text("  Удалить")
                    }
                }
            }
        }
    }
}
