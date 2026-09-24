package dev.asimonyan.studyflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.asimonyan.studyflow.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TaskUiState(val tasks: List<Task> = emptyList(), val loading: Boolean = true, val error: Boolean = false)

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    val state = repository.tasks.map { TaskUiState(tasks = it, loading = false) }
        .catch { emit(TaskUiState(loading = false, error = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskUiState())

    private val messagesChannel = Channel<String>(Channel.BUFFERED)
    val messages = messagesChannel.receiveAsFlow()

    fun save(task: Task, onSuccess: () -> Unit) = perform {
        repository.save(task)
        onSuccess()
    }
    fun toggle(id: Long) = perform { repository.toggle(id) }
    fun delete(id: Long) = perform { repository.delete(id) }

    private fun perform(action: suspend () -> Unit) = viewModelScope.launch {
        try { action() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { messagesChannel.send("Не удалось сохранить изменение. Попробуйте ещё раз.") }
    }
}
