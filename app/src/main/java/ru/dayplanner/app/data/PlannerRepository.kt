package ru.dayplanner.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import ru.dayplanner.app.domain.Completion
import ru.dayplanner.app.domain.DayKey
import ru.dayplanner.app.domain.EditScope
import ru.dayplanner.app.domain.Photo
import ru.dayplanner.app.domain.PlannerData
import ru.dayplanner.app.domain.RepeatMode
import ru.dayplanner.app.domain.SkippedDay
import ru.dayplanner.app.domain.Task
import ru.dayplanner.app.domain.Template

class PlannerRepository(
    private val db: AppDatabase,
    private val photoStore: PhotoStore,
    scope: CoroutineScope,
    private val onChanged: () -> Unit,
) {
    private val dao = db.dao()

    /** Все данные для экранов; null, пока идёт первая загрузка. */
    val data: StateFlow<PlannerData?> = combine(
        dao.observeTasks(),
        dao.observeSkipped(),
        dao.observeCompletions(),
    ) { tasks, skipped, completions ->
        val result: PlannerData? = PlannerData(
            tasks,
            skipped.mapTo(HashSet()) { DayKey(it.taskId, it.day) },
            completions.mapTo(HashSet()) { DayKey(it.taskId, it.day) },
        )
        result
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, null)

    val templates: Flow<List<Template>> =
        dao.observeTemplates().map { list -> list.sortedBy { it.title.lowercase() } }

    suspend fun snapshot(): PlannerData = PlannerData(
        dao.getTasks(),
        dao.getSkipped().mapTo(HashSet()) { DayKey(it.taskId, it.day) },
        dao.getCompletions().mapTo(HashSet()) { DayKey(it.taskId, it.day) },
    )

    /** Для мест, где нельзя приостанавливаться (фабрика списка виджета). Не вызывать из главного потока. */
    fun snapshotBlocking(): PlannerData = PlannerData(
        dao.getTasksBlocking(),
        dao.getSkippedBlocking().mapTo(HashSet()) { DayKey(it.taskId, it.day) },
        dao.getCompletionsBlocking().mapTo(HashSet()) { DayKey(it.taskId, it.day) },
    )

    suspend fun getTask(id: Long): Task? = dao.getTask(id)

    suspend fun getPhotoNames(taskId: Long): List<String> = dao.getPhotos(taskId).map { it.fileName }

    fun observePhotoNames(taskId: Long): Flow<List<String>> =
        dao.observePhotos(taskId).map { list -> list.map { it.fileName } }

    suspend fun setDone(taskId: Long, day: Long, done: Boolean) {
        if (done) dao.insertCompletion(Completion(taskId, day)) else dao.deleteCompletion(taskId, day)
        onChanged()
    }

    suspend fun createTask(task: Task, photos: List<String>): Long {
        val id = db.withTransaction {
            val newId = dao.insertTask(task.copy(id = 0, createdAt = System.currentTimeMillis()))
            writePhotos(newId, photos)
            newId
        }
        onChanged()
        return id
    }

    /**
     * Сохраняет изменения дела. Для повторяющегося дела [scope] определяет,
     * меняется только день [day] или этот день и все следующие.
     */
    suspend fun updateTask(original: Task, edited: Task, photos: List<String>, day: Long, scope: EditScope) {
        val oldPhotos = getPhotoNames(original.id)
        db.withTransaction {
            val current = dao.getTask(original.id) ?: return@withTransaction
            val effective = when {
                !current.isRepeating -> EditScope.ALL
                scope == EditScope.THIS_AND_FOLLOWING && day <= current.startDay -> EditScope.ALL
                else -> scope
            }
            when (effective) {
                EditScope.ALL -> {
                    dao.updateTask(
                        edited.copy(
                            id = current.id,
                            startDay = current.startDay,
                            endDay = if (edited.isRepeating) current.endDay else null,
                            createdAt = current.createdAt,
                        ),
                    )
                    dao.deletePhotosOf(current.id)
                    writePhotos(current.id, photos)
                }

                EditScope.THIS_DAY -> {
                    val wasDone = dao.countCompletion(current.id, day) > 0
                    dao.insertSkipped(SkippedDay(current.id, day))
                    dao.deleteCompletion(current.id, day)
                    val newId = dao.insertTask(
                        edited.copy(
                            id = 0,
                            startDay = day,
                            endDay = null,
                            repeatMode = RepeatMode.NONE,
                            weekdaysMask = 0,
                            createdAt = current.createdAt,
                        ),
                    )
                    writePhotos(newId, photos)
                    if (wasDone) dao.insertCompletion(Completion(newId, day))
                }

                EditScope.THIS_AND_FOLLOWING -> {
                    dao.updateTask(current.copy(endDay = day - 1))
                    val newId = dao.insertTask(
                        edited.copy(
                            id = 0,
                            startDay = day,
                            endDay = if (edited.isRepeating) current.endDay else null,
                            createdAt = current.createdAt,
                        ),
                    )
                    writePhotos(newId, photos)
                    dao.moveCompletions(current.id, newId, day)
                    dao.moveSkipped(current.id, newId, day)
                }
            }
        }
        cleanupFiles(oldPhotos - photos.toSet())
        onChanged()
    }

    suspend fun deleteTask(taskId: Long, day: Long, scope: EditScope) {
        val photos = getPhotoNames(taskId)
        var removedWhole = false
        db.withTransaction {
            val current = dao.getTask(taskId) ?: return@withTransaction
            val whole = !current.isRepeating || scope == EditScope.ALL ||
                (scope == EditScope.THIS_AND_FOLLOWING && day <= current.startDay)
            when {
                whole -> {
                    dao.deleteTask(current.id)
                    removedWhole = true
                }

                scope == EditScope.THIS_DAY -> {
                    dao.insertSkipped(SkippedDay(current.id, day))
                    dao.deleteCompletion(current.id, day)
                }

                else -> {
                    dao.updateTask(current.copy(endDay = day - 1))
                    dao.deleteCompletionsFrom(current.id, day)
                    dao.deleteSkippedFrom(current.id, day)
                }
            }
        }
        if (removedWhole) cleanupFiles(photos)
        onChanged()
    }

    // --- Типовые дела ---

    suspend fun getTemplate(id: Long): Template? = dao.getTemplate(id)

    suspend fun saveTemplate(template: Template) {
        if (template.id == 0L) dao.insertTemplate(template) else dao.updateTemplate(template)
    }

    suspend fun deleteTemplate(id: Long) = dao.deleteTemplate(id)

    // --- Вспомогательное ---

    private suspend fun writePhotos(taskId: Long, names: List<String>) {
        if (names.isEmpty()) return
        dao.insertPhotos(names.mapIndexed { index, name -> Photo(taskId = taskId, fileName = name, position = index) })
    }

    /** Удаляет файлы фото, на которые больше не ссылается ни одно дело. */
    private suspend fun cleanupFiles(names: Collection<String>) {
        if (names.isEmpty()) return
        val unused = names.toSet().filter { dao.countPhotoRefs(it) == 0 }
        if (unused.isEmpty()) return
        withContext(Dispatchers.IO) { unused.forEach { photoStore.delete(it) } }
    }
}
