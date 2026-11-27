package ru.dayplanner.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

data class DayKey(val taskId: Long, val day: Long)

/** Дело в конкретный день. */
data class Occurrence(val task: Task, val day: Long, val done: Boolean)

data class DayStat(val day: Long, val planned: Int, val done: Int)

data class PeriodStat(val days: List<DayStat>) {
    val planned: Int = days.sumOf { it.planned }
    val done: Int = days.sumOf { it.done }
    val percent: Int = if (planned == 0) 0 else Math.round(done * 100f / planned)
}

object TimeText {
    fun minutes(value: Int): String = String.format(Locale.ROOT, "%02d:%02d", value / 60, value % 60)

    /** Подпись времени в строке дела: пусто, «09:30» или «09:30–11:00». */
    fun label(task: Task): String = when (task.timeMode) {
        TimeMode.NONE -> ""
        TimeMode.AT -> minutes(task.startMinutes)
        TimeMode.RANGE -> minutes(task.startMinutes) + "–" + minutes(task.endMinutes)
    }
}

/** Чистая логика расписания: без Android, покрыта модульными тестами. */
object Schedule {

    /** 1 — понедельник … 7 — воскресенье. */
    fun dayOfWeek(day: Long): Int = Math.floorMod(day + 3, 7L).toInt() + 1

    fun weekdayBit(dayOfWeek: Int): Int = 1 shl (dayOfWeek - 1)

    /** Показывается ли дело в указанный день. */
    fun occursOn(task: Task, day: Long, skipped: Set<DayKey> = emptySet()): Boolean {
        if (day < task.startDay) return false
        if (task.repeatMode == RepeatMode.NONE) return day == task.startDay
        val end = task.endDay
        if (end != null && day > end) return false
        if (task.repeatMode == RepeatMode.WEEKDAYS) {
            val bit = weekdayBit(dayOfWeek(day))
            if ((task.weekdaysMask and bit) == 0) return false
        }
        return DayKey(task.id, day) !in skipped
    }

    /** Порядок списка дня: сначала дела без времени, затем по времени начала. */
    val dayOrder: Comparator<Occurrence> = compareBy<Occurrence>(
        { if (it.task.timeMode == TimeMode.NONE) 0 else 1 },
        { if (it.task.timeMode == TimeMode.NONE) 0 else it.task.startMinutes },
        {
            when (it.task.timeMode) {
                TimeMode.NONE -> 0
                TimeMode.AT -> it.task.startMinutes
                TimeMode.RANGE -> it.task.endMinutes
            }
        },
        { it.task.createdAt },
        { it.task.id },
    )

    fun sortForDay(items: List<Occurrence>): List<Occurrence> = items.sortedWith(dayOrder)

    /** Минуты от начала суток, в которые должен сработать сигнал, или null, если сигнала нет. */
    fun signalMinutes(task: Task, kind: SignalKind): Int? = when {
        task.timeMode == TimeMode.NONE -> null
        kind == SignalKind.START -> if (task.signalStart) task.startMinutes else null
        else -> if (task.signalEnd && task.timeMode == TimeMode.RANGE) task.endMinutes else null
    }

    /**
     * Время ближайшего сигнала строго после [now].
     * Пропускаются дни без дела, дни-исключения и дни, где дело уже отмечено выполненным.
     */
    fun nextSignal(
        task: Task,
        kind: SignalKind,
        now: LocalDateTime,
        skipped: Set<DayKey> = emptySet(),
        completions: Set<DayKey> = emptySet(),
        horizonDays: Int = 400,
    ): LocalDateTime? {
        val minutes = signalMinutes(task, kind) ?: return null
        val today = now.toLocalDate().toEpochDay()
        val first = maxOf(today, task.startDay)
        val last = if (task.repeatMode == RepeatMode.NONE) {
            task.startDay
        } else {
            minOf(task.endDay ?: Long.MAX_VALUE, today + horizonDays)
        }
        var day = first
        while (day <= last) {
            if (occursOn(task, day, skipped) && DayKey(task.id, day) !in completions) {
                val at = LocalDate.ofEpochDay(day).atTime(minutes / 60, minutes % 60)
                if (at.isAfter(now)) return at
            }
            day++
        }
        return null
    }
}

/** Снимок всех данных, нужных для построения списков, меток календаря и статистики. */
class PlannerData(
    val tasks: List<Task>,
    val skipped: Set<DayKey>,
    val completions: Set<DayKey>,
) {
    fun occurrencesOn(day: Long): List<Occurrence> = Schedule.sortForDay(
        tasks.filter { Schedule.occursOn(it, day, skipped) }
            .map { Occurrence(it, day, DayKey(it.id, day) in completions) },
    )

    fun hasTasksOn(day: Long): Boolean = tasks.any { Schedule.occursOn(it, day, skipped) }

    /** Дни из промежутка [fromDay, toDay], на которые есть хотя бы одно дело. */
    fun markedDays(fromDay: Long, toDay: Long): Set<Long> {
        val result = HashSet<Long>()
        var day = fromDay
        while (day <= toDay) {
            if (hasTasksOn(day)) result.add(day)
            day++
        }
        return result
    }

    /** Статистика по дням промежутка [fromDay, toDay]. */
    fun stats(fromDay: Long, toDay: Long): PeriodStat {
        val days = ArrayList<DayStat>()
        var day = fromDay
        while (day <= toDay) {
            val items = occurrencesOn(day)
            days.add(DayStat(day, items.size, items.count { it.done }))
            day++
        }
        return PeriodStat(days)
    }

    companion object {
        val EMPTY = PlannerData(emptyList(), emptySet(), emptySet())
    }
}
