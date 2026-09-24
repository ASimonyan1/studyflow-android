package dev.asimonyan.studyflow

import dev.asimonyan.studyflow.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TaskRulesTest {
    private val today = LocalDate.of(2026, 9, 24)
    @Test fun `today is not overdue but yesterday is`() {
        assertFalse(Task(title = "Today", dueEpochDay = today.toEpochDay()).isOverdue(today))
        assertTrue(Task(title = "Past", dueEpochDay = today.minusDays(1).toEpochDay()).isOverdue(today))
    }
    @Test fun `completed and undated tasks are never overdue`() {
        assertFalse(Task(title = "Done", dueEpochDay = 1, completed = true).isOverdue(today))
        assertFalse(Task(title = "Later").isOverdue(today))
    }
    @Test fun `search ignores case and outer whitespace and includes subject`() {
        val tasks = listOf(Task(id = 1, title = "Лабораторная", subject = "Kotlin"), Task(id = 2, title = "Математика"))
        assertEquals(listOf(tasks[0]), selectTasks(tasks, "  KOTLIN ", TaskFilter.ALL))
        assertEquals(listOf(tasks[1]), selectTasks(tasks, "матем", TaskFilter.ALL))
    }
    @Test fun `status filtering is combined with search`() {
        val tasks = listOf(Task(id = 1, title = "Test"), Task(id = 2, title = "Test", completed = true))
        assertEquals(listOf(tasks[0]), selectTasks(tasks, "test", TaskFilter.ACTIVE))
        assertEquals(listOf(tasks[1]), selectTasks(tasks, "test", TaskFilter.COMPLETED))
        assertTrue(selectTasks(tasks, "missing", TaskFilter.ALL).isEmpty())
    }
    @Test fun `urgent active tasks sort before undated and completed`() {
        val tasks = listOf(Task(id = 1, title = "Undated"), Task(id = 2, title = "Later", dueEpochDay = 200),
            Task(id = 3, title = "Done", dueEpochDay = 1, completed = true), Task(id = 4, title = "Urgent", dueEpochDay = 100))
        assertEquals(listOf(4L, 2L, 1L, 3L), selectTasks(tasks, "", TaskFilter.ALL).map { it.id })
    }
    @Test fun `blank and oversized titles cannot be saved`() {
        assertFalse(validTitle(" \n "))
        assertFalse(validTitle("x".repeat(121)))
        assertTrue(validTitle("  Задача  "))
        assertTrue(validTitle("x".repeat(120)))
    }
}
