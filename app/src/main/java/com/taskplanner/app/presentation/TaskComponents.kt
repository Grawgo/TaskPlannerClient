package com.taskplanner.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskplanner.app.domain.model.Task
import com.taskplanner.app.domain.model.TaskPriority
import com.taskplanner.app.domain.model.TaskStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlannerHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 16.dp)) {
        if (onBack != null) IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
        }
        Column(modifier = Modifier.padding(start = if (onBack == null) 0.dp else 8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CenterMessage(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
fun TaskCard(task: Task, onOpen: () -> Unit, onToggle: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable(onClick = onOpen)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onToggle != null) Checkbox(checked = task.status == TaskStatus.DONE, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(task.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                Text("${task.status.label()} · ${task.priority.label()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                task.dueAt?.let {
                    Text("Срок: ${formatDueAt(it)}", style = MaterialTheme.typography.labelMedium,
                        color = if (task.isOverdue()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun TaskPriority.label(): String = when (this) {
    TaskPriority.LOW -> "Низкий приоритет"
    TaskPriority.MEDIUM -> "Средний приоритет"
    TaskPriority.HIGH -> "Высокий приоритет"
}

fun TaskStatus.label(): String = when (this) {
    TaskStatus.TODO -> "К выполнению"
    TaskStatus.IN_PROGRESS -> "В работе"
    TaskStatus.DONE -> "Выполнена"
}

fun formatDueAt(value: String): String = try {
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault()).format(Instant.parse(value))
} catch (_: Exception) { value }

private fun Task.isOverdue(): Boolean = status != TaskStatus.DONE && dueAt?.let {
    try { Instant.parse(it).isBefore(Instant.now()) } catch (_: Exception) { false }
} == true
