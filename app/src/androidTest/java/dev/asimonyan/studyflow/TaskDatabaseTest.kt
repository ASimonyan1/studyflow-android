package dev.asimonyan.studyflow

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.asimonyan.studyflow.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDatabaseTest {
    private lateinit var db: TaskDatabase
    @Before fun setUp() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build() }
    @After fun tearDown() { db.close() }
    @Test fun createEditToggleDelete() = runTest {
        val repository = RoomTaskRepository(db.tasks())
        repository.save(Task(title = "  Лабораторная  ", subject = " Kotlin ", dueEpochDay = 20000))
        val saved = repository.tasks.first().single()
        assertEquals("Лабораторная", saved.title)
        assertEquals("Kotlin", saved.subject)
        repository.save(saved.copy(title = "Практика", dueEpochDay = null))
        repository.toggle(saved.id)
        val updated = repository.tasks.first().single()
        assertEquals("Практика", updated.title)
        assertNull(updated.dueEpochDay)
        assertTrue(updated.completed)
        repository.toggle(saved.id)
        assertFalse(repository.tasks.first().single().completed)
        repository.delete(saved.id)
        assertTrue(repository.tasks.first().isEmpty())
    }
}
