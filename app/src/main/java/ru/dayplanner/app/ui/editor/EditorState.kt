package ru.dayplanner.app.ui.editor

import ru.dayplanner.app.domain.RepeatMode
import ru.dayplanner.app.domain.SignalType
import ru.dayplanner.app.domain.Task
import ru.dayplanner.app.domain.TimeMode
import java.io.Serializable

/**
 * Все поля формы дела. Serializable — чтобы форма переживала выгрузку приложения из памяти
 * (например, пока открыта камера).
 */
data class EditorState(
    val title: String = "",
    val description: String = "",
    val timeMode: TimeMode = TimeMode.NONE,
    val startMinutes: Int = 9 * 60,
    val endMinutes: Int = 10 * 60,
    val photos: List<String> = emptyList(),
    val signalStart: Boolean = false,
    val signalEnd: Boolean = false,
    val signalType: SignalType = SignalType.NOTIFICATION,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val weekdaysMask: Int = 0,
    val saveAsTemplate: Boolean = false,
    val titleError: Boolean = false,
    val timeError: Boolean = false,
    val weekdaysError: Boolean = false,
) : Serializable {
    fun toTask(day: Long): Task = Task(
        title = title.trim(),
        description = description.trim(),
        timeMode = timeMode,
        startMinutes = if (timeMode == TimeMode.NONE) 0 else startMinutes,
        endMinutes = if (timeMode == TimeMode.RANGE) endMinutes else 0,
        startDay = day,
        repeatMode = repeatMode,
        weekdaysMask = if (repeatMode == RepeatMode.WEEKDAYS) weekdaysMask else 0,
        signalStart = timeMode != TimeMode.NONE && signalStart,
        signalEnd = timeMode == TimeMode.RANGE && signalEnd,
        signalType = signalType,
    )

    companion object {
        private const val serialVersionUID = 1L
        const val LAST_MINUTE = 23 * 60 + 59

        fun from(task: Task, photos: List<String>): EditorState {
            val start = if (task.timeMode == TimeMode.NONE) 9 * 60 else task.startMinutes
            val end = if (task.timeMode == TimeMode.RANGE) {
                task.endMinutes
            } else {
                minOf(start + 60, LAST_MINUTE)
            }
            return EditorState(
                title = task.title,
                description = task.description,
                timeMode = task.timeMode,
                startMinutes = start,
                endMinutes = end,
                photos = ArrayList(photos),
                signalStart = task.signalStart,
                signalEnd = task.signalEnd,
                signalType = task.signalType,
                repeatMode = task.repeatMode,
                weekdaysMask = task.weekdaysMask,
            )
        }
    }
}
