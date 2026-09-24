package dev.asimonyan.studyflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import dev.asimonyan.studyflow.data.*
import dev.asimonyan.studyflow.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: TaskViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = Room.databaseBuilder(applicationContext, TaskDatabase::class.java, "studyflow.db").build()
                    return TaskViewModel(RoomTaskRepository(database.tasks())) as T
                }
            })
            val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFFA5D5BD))
            else lightColorScheme(primary = Color(0xFF315B4D), background = Color(0xFFF7F8F2), surface = Color(0xFFF7F8F2))
            MaterialTheme(colorScheme = colors) { StudyFlowScreen(model) }
        }
    }
}
