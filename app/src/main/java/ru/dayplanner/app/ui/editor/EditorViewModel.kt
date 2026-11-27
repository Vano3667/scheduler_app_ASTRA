package ru.dayplanner.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import ru.dayplanner.app.AppContainer
import ru.dayplanner.app.domain.EditScope
import ru.dayplanner.app.domain.RepeatMode
import ru.dayplanner.app.domain.Task
import ru.dayplanner.app.domain.Template
import ru.dayplanner.app.domain.TimeMode
import java.time.LocalDate
import java.time.LocalTime

class EditorViewModel(
    private val container: AppContainer,
    private val taskId: Long,
    val day: Long,
    private val handle: SavedStateHandle,
) : ViewModel() {

    var state by mutableStateOf(EditorState())
        private set

    var loaded by mutableStateOf(taskId <= 0)
        private set

    /** Дело не найдено (например, уже удалено). */
    var missing by mutableStateOf(false)
        private set

    var original: Task? = null
        private set

    private var saving = false

    val isNew: Boolean get() = taskId <= 0

    /** Нужно ли спросить «только этот день / этот и все следующие». */
    val needsScopeQuestion: Boolean get() = original?.isRepeating == true

    init {
        // Форма, сохранённая перед выгрузкой приложения из памяти (если была).
        val restored: EditorState? = try {
            handle.get<EditorState>(KEY_STATE)
        } catch (e: Exception) {
            null
        }
        if (restored != null) state = restored

        if (taskId <= 0) {
            if (restored == null) {
                val start = if (day == LocalDate.now().toEpochDay()) {
                    minOf((LocalTime.now().hour + 1) * 60, 23 * 60)
                } else {
                    9 * 60
                }
                state = EditorState(
                    startMinutes = start,
                    endMinutes = minOf(start + 60, EditorState.LAST_MINUTE),
                )
            }
        } else {
            viewModelScope.launch {
                val task = container.repository.getTask(taskId)
                if (task == null) {
                    missing = true
                } else {
                    original = task
                    if (restored == null) {
                        state = EditorState.from(task, container.repository.getPhotoNames(taskId))
                    }
                }
                loaded = true
            }
        }
    }

    fun update(new: EditorState) {
        var s = new
        if (s.timeMode == TimeMode.RANGE && state.timeMode != TimeMode.RANGE && s.endMinutes <= s.startMinutes) {
            s = s.copy(endMinutes = minOf(s.startMinutes + 60, EditorState.LAST_MINUTE))
        }
        if (s.titleError && s.title.isNotBlank()) s = s.copy(titleError = false)
        if (s.timeError && (s.timeMode != TimeMode.RANGE || s.endMinutes > s.startMinutes)) {
            s = s.copy(timeError = false)
        }
        if (s.weekdaysError && (s.repeatMode != RepeatMode.WEEKDAYS || s.weekdaysMask != 0)) {
            s = s.copy(weekdaysError = false)
        }
        state = s
        handle[KEY_STATE] = s
    }

    /** Проверяет форму и подсвечивает ошибки. */
    fun validate(): Boolean {
        val s = state
        val titleError = s.title.isBlank()
        val timeError = s.timeMode == TimeMode.RANGE && s.endMinutes <= s.startMinutes
        val weekdaysError = s.repeatMode == RepeatMode.WEEKDAYS && s.weekdaysMask == 0
        state = s.copy(titleError = titleError, timeError = timeError, weekdaysError = weekdaysError)
        return !(titleError || timeError || weekdaysError)
    }

    fun save(scope: EditScope, onSaved: () -> Unit) {
        if (saving || !loaded) return
        saving = true
        val s = state
        val orig = original
        container.launch {
            try {
                val repo = container.repository
                val edited = s.toTask(day)
                if (orig == null) {
                    repo.createTask(edited, s.photos)
                } else {
                    repo.updateTask(orig, edited, s.photos, day, scope)
                }
                if (s.saveAsTemplate) {
                    repo.saveTemplate(
                        Template(
                            title = edited.title,
                            description = edited.description,
                            signalStart = s.signalStart,
                            signalEnd = s.signalEnd,
                            signalType = s.signalType,
                        ),
                    )
                }
                onSaved()
            } finally {
                saving = false
            }
        }
    }

    companion object {
        private const val KEY_STATE = "editor_state"

        fun factory(container: AppContainer, taskId: Long, day: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { EditorViewModel(container, taskId, day, createSavedStateHandle()) }
            }
    }
}
