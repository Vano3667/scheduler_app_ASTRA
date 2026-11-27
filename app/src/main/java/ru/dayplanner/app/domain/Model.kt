package ru.dayplanner.app.domain

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TimeMode { NONE, AT, RANGE }

enum class RepeatMode { NONE, DAILY, WEEKDAYS }

enum class SignalType { NOTIFICATION, ALARM }

enum class SignalKind { START, END }

/** К чему применить изменение или удаление повторяющегося дела. */
enum class EditScope { ALL, THIS_DAY, THIS_AND_FOLLOWING }

/**
 * Дело. Даты хранятся как номер дня от 1970-01-01 (LocalDate.toEpochDay),
 * время — в минутах от начала суток.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val timeMode: TimeMode = TimeMode.NONE,
    val startMinutes: Int = 0,
    val endMinutes: Int = 0,
    /** День дела; для повторяющегося — первый день. */
    val startDay: Long,
    /** Последний день повторяющегося дела включительно, null — без ограничения. */
    val endDay: Long? = null,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    /** Бит 0 — понедельник … бит 6 — воскресенье. */
    val weekdaysMask: Int = 0,
    val signalStart: Boolean = false,
    val signalEnd: Boolean = false,
    val signalType: SignalType = SignalType.NOTIFICATION,
    val createdAt: Long = 0,
) {
    val isRepeating: Boolean get() = repeatMode != RepeatMode.NONE
}

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val fileName: String,
    val position: Int = 0,
)

/** Отметка «выполнено» для конкретного дня. */
@Entity(
    tableName = "completions",
    primaryKeys = ["taskId", "day"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Completion(val taskId: Long, val day: Long)

/** День, в который повторяющееся дело не показывается (удалено или заменено на этот день). */
@Entity(
    tableName = "skipped_days",
    primaryKeys = ["taskId", "day"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SkippedDay(val taskId: Long, val day: Long)

/** Типовое дело (шаблон). */
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val signalStart: Boolean = false,
    val signalEnd: Boolean = false,
    val signalType: SignalType = SignalType.NOTIFICATION,
)
