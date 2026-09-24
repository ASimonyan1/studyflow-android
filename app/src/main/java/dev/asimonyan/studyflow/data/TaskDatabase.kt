package dev.asimonyan.studyflow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun observe(): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("UPDATE tasks SET completed = NOT completed WHERE id = :id")
    suspend fun toggle(id: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [Task::class], version = 1, exportSchema = true)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
}

interface TaskRepository {
    val tasks: Flow<List<Task>>
    suspend fun save(task: Task)
    suspend fun toggle(id: Long)
    suspend fun delete(id: Long)
}

class RoomTaskRepository(private val dao: TaskDao) : TaskRepository {
    override val tasks = dao.observe()
    override suspend fun save(task: Task) {
        require(validTitle(task.title))
        val normalized = task.copy(title = task.title.trim(), subject = task.subject.trim())
        if (task.id == 0L) dao.insert(normalized) else dao.update(normalized)
    }
    override suspend fun toggle(id: Long) = dao.toggle(id)
    override suspend fun delete(id: Long) = dao.delete(id)
}
