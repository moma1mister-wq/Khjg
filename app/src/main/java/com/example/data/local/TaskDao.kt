package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isEssential DESC, targetTime ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dayOfWeek = 0 OR dayOfWeek = :dayOfWeek ORDER BY isEssential DESC, targetTime ASC")
    fun getTasksForDay(dayOfWeek: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE hasReminder = 1")
    suspend fun getTasksWithRemindersSync(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE isEssential = 1")
    suspend fun countEssentialTasks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompletion(id: Int, completed: Boolean)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id AND isEssential = 0")
    suspend fun deleteNonEssentialTaskById(id: Int)
}
