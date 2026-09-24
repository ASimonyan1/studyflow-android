package dev.asimonyan.studyflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "",
    val dueEpochDay: Long? = null,
    val completed: Boolean = false,
)

enum class TaskFilter(val label: String) {
    ACTIVE("В работе"), ALL("Все"), COMPLETED("Готово")
}

/** Pure query logic; a deadline is overdue only after its calendar day has ended. */
fun Task.isOverdue(today: LocalDate): Boolean =
    !completed && dueEpochDay != null && dueEpochDay < today.toEpochDay()

fun selectTasks(tasks: List<Task>, query: String, filter: TaskFilter): List<Task> {
    val needle = query.trim()
    return tasks.filter { task ->
        val matchesStatus = when (filter) {
            TaskFilter.ALL -> true
            TaskFilter.ACTIVE -> !task.completed
            TaskFilter.COMPLETED -> task.completed
        }
        matchesStatus && (task.title.contains(needle, ignoreCase = true) ||
            task.subject.contains(needle, ignoreCase = true))
    }.sortedWith(compareBy<Task> { it.completed }
        .thenBy { it.dueEpochDay ?: Long.MAX_VALUE }.thenByDescending { it.id })
}

fun validTitle(raw: String): Boolean = raw.trim().length in 1..120
