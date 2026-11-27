package ru.dayplanner.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.dayplanner.app.domain.Completion
import ru.dayplanner.app.domain.Photo
import ru.dayplanner.app.domain.SkippedDay
import ru.dayplanner.app.domain.Task
import ru.dayplanner.app.domain.Template

@Dao
interface PlannerDao {

    // --- Дела ---

    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getTasks(): List<Task>

    @Query("SELECT * FROM tasks")
    fun getTasksBlocking(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): Task?

    @Insert
    suspend fun insertTask(task: Task): Long

    @Insert
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // --- Фото ---

    @Query("SELECT * FROM photos WHERE taskId = :taskId ORDER BY position, id")
    fun observePhotos(taskId: Long): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE taskId = :taskId ORDER BY position, id")
    suspend fun getPhotos(taskId: Long): List<Photo>

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotos(): List<Photo>

    @Insert
    suspend fun insertPhotos(photos: List<Photo>)

    @Query("DELETE FROM photos WHERE taskId = :taskId")
    suspend fun deletePhotosOf(taskId: Long)

    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos()

    @Query("SELECT COUNT(*) FROM photos WHERE fileName = :fileName")
    suspend fun countPhotoRefs(fileName: String): Int

    // --- Отметки «выполнено» ---

    @Query("SELECT * FROM completions")
    fun observeCompletions(): Flow<List<Completion>>

    @Query("SELECT * FROM completions")
    suspend fun getCompletions(): List<Completion>

    @Query("SELECT * FROM completions")
    fun getCompletionsBlocking(): List<Completion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: Completion)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletions(completions: List<Completion>)

    @Query("SELECT COUNT(*) FROM completions WHERE taskId = :taskId AND day = :day")
    suspend fun countCompletion(taskId: Long, day: Long): Int

    @Query("DELETE FROM completions WHERE taskId = :taskId AND day = :day")
    suspend fun deleteCompletion(taskId: Long, day: Long)

    @Query("DELETE FROM completions WHERE taskId = :taskId AND day >= :fromDay")
    suspend fun deleteCompletionsFrom(taskId: Long, fromDay: Long)

    @Query("UPDATE completions SET taskId = :newId WHERE taskId = :oldId AND day >= :fromDay")
    suspend fun moveCompletions(oldId: Long, newId: Long, fromDay: Long)

    @Query("DELETE FROM completions")
    suspend fun deleteAllCompletions()

    // --- Дни-исключения повторяющихся дел ---

    @Query("SELECT * FROM skipped_days")
    fun observeSkipped(): Flow<List<SkippedDay>>

    @Query("SELECT * FROM skipped_days")
    suspend fun getSkipped(): List<SkippedDay>

    @Query("SELECT * FROM skipped_days")
    fun getSkippedBlocking(): List<SkippedDay>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkipped(skipped: SkippedDay)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkippedList(skipped: List<SkippedDay>)

    @Query("DELETE FROM skipped_days WHERE taskId = :taskId AND day >= :fromDay")
    suspend fun deleteSkippedFrom(taskId: Long, fromDay: Long)

    @Query("UPDATE skipped_days SET taskId = :newId WHERE taskId = :oldId AND day >= :fromDay")
    suspend fun moveSkipped(oldId: Long, newId: Long, fromDay: Long)

    @Query("DELETE FROM skipped_days")
    suspend fun deleteAllSkipped()

    // --- Типовые дела ---

    @Query("SELECT * FROM templates")
    fun observeTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates")
    suspend fun getTemplates(): List<Template>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplate(id: Long): Template?

    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Insert
    suspend fun insertTemplates(templates: List<Template>)

    @Update
    suspend fun updateTemplate(template: Template)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("DELETE FROM templates")
    suspend fun deleteAllTemplates()
}
