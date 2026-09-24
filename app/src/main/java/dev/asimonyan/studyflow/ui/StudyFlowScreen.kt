package dev.asimonyan.studyflow.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.asimonyan.studyflow.data.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyFlowScreen(model: TaskViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.ACTIVE.name) }
    val filter = TaskFilter.valueOf(filterName)
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { while (true) { delay(60_000); today = LocalDate.now() } }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(it) } }
    val visible = remember(state.tasks, query, filter) { selectTasks(state.tasks, query, filter) }
    val active = state.tasks.count { !it.completed }
    val overdue = state.tasks.count { it.isOverdue(today) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("StudyFlow", fontWeight = FontWeight.Bold) },
            actions = { Text("УЧЁБА В ПОРЯДКЕ", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 16.dp)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editingId = 0L },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) }, text = { Text("Новая задача") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Меньше хаоса.\nБольше сделанного.", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Задачи и сроки — под рукой, даже без сети.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Summary("В работе", active.toString())
                        Summary("Просрочено", overdue.toString())
                        Summary("Готово", state.tasks.count { it.completed }.toString())
                    }
                }
            }
            item {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    label = { Text("Поиск по задаче или предмету") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, "Очистить поиск") } }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskFilter.entries.forEach { item ->
                        FilterChip(selected = item == filter, onClick = { filterName = item.name }, label = { Text(item.label) })
                    }
                }
            }
            when {
                state.loading -> item { CircularProgressIndicator() }
                state.error -> item { Text("Не удалось прочитать задачи. Закройте и снова откройте приложение.") }
                visible.isEmpty() -> item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(if (query.isNotBlank()) "Ничего не найдено" else "Здесь пока нет задач",
                            style = MaterialTheme.typography.titleLarge)
                        Text(if (query.isNotBlank()) "Попробуйте другое название или предмет." else "Начните с одного небольшого дела.")
                    }
                }
                else -> items(visible, key = { it.id }) { task ->
                    TaskCard(task, today, onToggle = { model.toggle(task.id) },
                        onEdit = { editingId = task.id }, onDelete = { deletingId = task.id })
                }
            }
        }
    }
    editingId?.let { id ->
        val task = if (id == 0L) Task(title = "") else state.tasks.find { it.id == id }
        if (task != null) key(id) { TaskEditor(task, onDismiss = { editingId = null },
            onSave = { model.save(it) { editingId = null } }) }
    }
    deletingId?.let { id ->
        AlertDialog(onDismissRequest = { deletingId = null }, title = { Text("Удалить задачу?") },
            text = { Text("«${state.tasks.find { it.id == id }?.title.orEmpty()}» будет удалена с устройства.") },
            confirmButton = { TextButton(onClick = { model.delete(id); deletingId = null }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Отмена") } })
    }
}

@Composable
private fun Summary(label: String, count: String) {
    Column { Text(count, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun TaskCard(task: Task, today: LocalDate, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(onClick = onEdit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                if (task.subject.isNotBlank()) Text(task.subject, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                task.dueEpochDay?.let { epoch ->
                    val prefix = if (task.isOverdue(today)) "Просрочено · " else "До "
                    Text(prefix + LocalDate.ofEpochDay(epoch).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (task.isOverdue(today)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Удалить: ${task.title}") }
        }
    }
}

@Composable
private fun TaskEditor(task: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var title by rememberSaveable { mutableStateOf(task.title) }
    var subject by rememberSaveable { mutableStateOf(task.subject) }
    var due by rememberSaveable { mutableStateOf(task.dueEpochDay) }
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (task.id == 0L) "Новая задача" else "Редактирование") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Что нужно сделать?") }, supportingText = { Text("${title.length}/120") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3)
                OutlinedTextField(value = subject, onValueChange = { if (it.length <= 60) subject = it },
                    label = { Text("Предмет (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = {
                    val selected = due?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
                    DatePickerDialog(context, { _, year, month, day ->
                        due = LocalDate.of(year, month + 1, day).toEpochDay()
                    }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
                }) { Icon(Icons.Outlined.DateRange, null); Spacer(Modifier.width(8.dp))
                    Text(due?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) } ?: "Выбрать срок") }
                if (due != null) TextButton(onClick = { due = null }) { Text("Убрать срок") }
            }
        },
        confirmButton = { TextButton(enabled = validTitle(title), onClick = { onSave(task.copy(title = title, subject = subject, dueEpochDay = due)) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}
