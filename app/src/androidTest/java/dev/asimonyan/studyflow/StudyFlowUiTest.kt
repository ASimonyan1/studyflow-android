package dev.asimonyan.studyflow

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.asimonyan.studyflow.data.*
import dev.asimonyan.studyflow.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File
import java.time.LocalDate

class StudyFlowUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: TaskDatabase
    @After fun close() { db.close() }
    @Test fun addSearchCompleteAndCapture() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaskDatabase::class.java).build()
        val today = LocalDate.now()
        runBlocking {
            db.tasks().insert(Task(title = "Разобрать обход графа", subject = "СиАОД", dueEpochDay = today.minusDays(1).toEpochDay()))
            db.tasks().insert(Task(title = "Практика по Kotlin", subject = "Android", dueEpochDay = today.toEpochDay()))
            db.tasks().insert(Task(title = "Подготовить курсовую", subject = "Программная инженерия", dueEpochDay = today.plusDays(3).toEpochDay()))
        }
        val model = TaskViewModel(RoomTaskRepository(db.tasks()))
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF315B4D), background = Color(0xFFF7F8F2), surface = Color(0xFFF7F8F2))) {
                StudyFlowScreen(model)
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Практика по Kotlin").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Практика по Kotlin").assertIsDisplayed()
        capture("studyflow-home.png")
        compose.onNodeWithText("Новая задача").performClick()
        compose.onNodeWithText("Что нужно сделать?").performTextInput("Тестовая задача")
        compose.onNodeWithText("Предмет (необязательно)").performTextInput("JUnit")
        capture("studyflow-editor.png")
        compose.onNodeWithText("Сохранить").performClick()
        compose.onNodeWithText("Поиск по задаче или предмету").performTextInput("JUnit")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Тестовая задача").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Тестовая задача").assertIsDisplayed()
        compose.onNode(isToggleable()).performClick()
        compose.onNode(hasText("Готово") and hasClickAction()).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Тестовая задача").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Тестовая задача").assertIsDisplayed()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
