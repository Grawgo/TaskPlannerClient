package com.taskplanner.app.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskplanner.app.domain.model.TaskPriority
import com.taskplanner.app.domain.model.TaskStatus
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        PlannerHeader(if (viewModel.editing) "Изменить задачу" else "Новая задача", onBack = onBack)
        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.title, onValueChange = viewModel::title,
                label = { Text("Название") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description, onValueChange = viewModel::description,
                label = { Text("Описание") }, minLines = 4, maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Срок выполнения", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val initial = state.dueAt?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull() }
                        ?: LocalDateTime.now().plusDays(1)
                    DatePickerDialog(context, { _, year, month, day ->
                        TimePickerDialog(context, { _, hour, minute ->
                            viewModel.dueAt(LocalDateTime.of(year, month + 1, day, hour, minute)
                                .atZone(ZoneId.systemDefault()).toInstant().toString())
                        }, initial.hour, initial.minute, true).show()
                    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }) { Text(state.dueAt?.let(::formatDueAt) ?: "Выбрать дату и время") }
                if (state.dueAt != null) TextButton(onClick = { viewModel.dueAt(null) }) { Text("Убрать") }
            }
            Text("Приоритет", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = state.priority == priority,
                        onClick = { viewModel.priority(priority) },
                        label = { Text(priority.label().removeSuffix(" приоритет")) },
                    )
                }
            }
            Text("Статус", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskStatus.entries.forEach { status ->
                    FilterChip(selected = state.status == status, onClick = { viewModel.status(status) }, label = { Text(status.label()) })
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { scope.launch { if (viewModel.save()) onBack() } },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (state.saving) "Сохраняем…" else "Сохранить") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SearchScreen(viewModel: SearchViewModel, onOpen: (String) -> Unit, onLogin: () -> Unit) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize()) {
        PlannerHeader("Поиск")
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("Найти задачу") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery(""); keyboard?.hide() }) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.search() }),
        )
        if (query.isNotBlank()) {
            Button(onClick = { keyboard?.hide(); viewModel.search() }, modifier = Modifier.padding(start = 20.dp, top = 12.dp)) { Text("Искать") }
        }
        when {
            state.loading -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.requiresLogin -> SearchFeedback(Modifier.weight(1f), "Войдите в аккаунт", "После входа вы сможете искать свои задачи", "Войти", onLogin)
            state.error != null -> {
                SearchFeedback(Modifier.weight(1f), "Не удалось выполнить поиск", state.error!!, "Обновить", viewModel::search)
            }
            query.isBlank() -> {
                if (history.isEmpty()) SearchFeedback(Modifier.weight(1f), "Поиск задач", "Введите название или часть описания")
                else LazyColumn(Modifier.weight(1f)) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Недавние запросы", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = viewModel::clearHistory) { Text("Очистить историю") }
                        }
                    }
                    items(history, key = { it }) { item ->
                        TextButton(
                            onClick = { viewModel.setQuery(item); viewModel.search() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        ) { Text(item) }
                    }
                }
            }
            state.searched && state.results.isEmpty() -> SearchFeedback(Modifier.weight(1f), "Ничего не найдено", "Попробуйте изменить запрос")
            state.searched -> LazyColumn(Modifier.weight(1f)) {
                items(state.results, key = { it.id }) { task ->
                    TaskCard(task, onOpen = { viewModel.openResult(); onOpen(task.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchFeedback(
    modifier: Modifier,
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier.verticalScroll(rememberScrollState())) {
        CenterMessage(title, message, action, onAction)
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onLogin: () -> Unit, onSignedOut: () -> Unit) {
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        PlannerHeader("Настройки")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Тёмная тема", style = MaterialTheme.typography.titleMedium)
                Text("Сохранить выбор оформления", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = darkMode, onCheckedChange = viewModel::setDarkMode)
        }
        Text("Аккаунт", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        if (user == null) {
            OutlinedButton(onClick = onLogin, modifier = Modifier.padding(horizontal = 24.dp)) { Text("Войти в аккаунт") }
        } else {
            Text(user?.email ?: "Аккаунт", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            OutlinedButton(
                onClick = { viewModel.signOut(); onSignedOut() },
                modifier = Modifier.padding(horizontal = 24.dp),
            ) { Text("Выйти из аккаунта") }
        }
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSuccess() }
    val title = when (state.mode) {
        AuthMode.LOGIN -> "Вход"
        AuthMode.REGISTER -> "Регистрация"
        AuthMode.RESET_PASSWORD -> "Сброс пароля"
    }
    Column(Modifier.fillMaxSize()) {
        PlannerHeader(title, "Планируйте свои задачи", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.configured) {
                Text(
                    "Авторизация недоступна: настройки Firebase не добавлены в приложение",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = state.email, onValueChange = viewModel::setEmail,
                label = { Text("Электронная почта") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = if (state.mode == AuthMode.RESET_PASSWORD) ImeAction.Done else ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.mode != AuthMode.RESET_PASSWORD) {
                OutlinedTextField(
                    value = state.password, onValueChange = viewModel::setPassword,
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (state.mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = state.confirmation, onValueChange = viewModel::setConfirmation,
                    label = { Text("Повторите пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(
                onClick = viewModel::submit,
                enabled = state.configured && !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.loading) CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                else Text(when (state.mode) {
                    AuthMode.LOGIN -> "Войти"
                    AuthMode.REGISTER -> "Создать аккаунт"
                    AuthMode.RESET_PASSWORD -> "Отправить письмо"
                })
            }
            when (state.mode) {
                AuthMode.LOGIN -> {
                    TextButton(onClick = { viewModel.setMode(AuthMode.REGISTER) }) { Text("Создать аккаунт") }
                    TextButton(onClick = { viewModel.setMode(AuthMode.RESET_PASSWORD) }) { Text("Забыли пароль?") }
                }
                AuthMode.REGISTER, AuthMode.RESET_PASSWORD -> {
                    TextButton(onClick = { viewModel.setMode(AuthMode.LOGIN) }) { Text("Вернуться ко входу") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
