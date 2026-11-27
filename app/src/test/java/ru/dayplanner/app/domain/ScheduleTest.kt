package ru.dayplanner.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleTest {

    /** Понедельник, 23 ноября 2026 года. */
    private val monday: Long = LocalDate.of(2026, 11, 23).toEpochDay()

    private fun task(
        id: Long,
        mode: TimeMode = TimeMode.NONE,
        start: Int = 0,
        end: Int = 0,
        day: Long = monday,
        repeat: RepeatMode = RepeatMode.NONE,
        mask: Int = 0,
        endDay: Long? = null,
        signalStart: Boolean = false,
        signalEnd: Boolean = false,
        createdAt: Long = id,
    ) = Task(
        id = id,
        title = "t$id",
        timeMode = mode,
        startMinutes = start,
        endMinutes = end,
        startDay = day,
        endDay = endDay,
        repeatMode = repeat,
        weekdaysMask = mask,
        signalStart = signalStart,
        signalEnd = signalEnd,
        createdAt = createdAt,
    )

    private fun at(day: Long, hour: Int, minute: Int): LocalDateTime =
        LocalDate.ofEpochDay(day).atTime(hour, minute)

    // --- День недели ---

    @Test
    fun dayOfWeek_matchesCalendar() {
        assertEquals(1, Schedule.dayOfWeek(monday))
        assertEquals(7, Schedule.dayOfWeek(monday + 6))
        assertEquals(4, Schedule.dayOfWeek(0))
        var day = -400L
        while (day < 30_000L) {
            assertEquals(LocalDate.ofEpochDay(day).dayOfWeek.value, Schedule.dayOfWeek(day))
            day += 37
        }
    }

    // --- Сортировка списка дня ---

    @Test
    fun sort_untimedFirst_thenByStartTime() {
        val items = listOf(
            Occurrence(task(1, createdAt = 5), monday, false),
            Occurrence(task(2, TimeMode.AT, start = 540), monday, false),
            Occurrence(task(3, TimeMode.RANGE, start = 540, end = 600), monday, false),
            Occurrence(task(4, TimeMode.AT, start = 510), monday, true),
            Occurrence(task(5, createdAt = 1), monday, false),
            Occurrence(task(6, TimeMode.RANGE, start = 540, end = 570), monday, false),
        )
        val sorted = Schedule.sortForDay(items).map { it.task.id }
        assertEquals(listOf(5L, 1L, 4L, 2L, 6L, 3L), sorted)
    }

    @Test
    fun sort_doneItemsKeepTheirPlace() {
        val items = listOf(
            Occurrence(task(1, TimeMode.AT, start = 600), monday, false),
            Occurrence(task(2, TimeMode.AT, start = 480), monday, true),
            Occurrence(task(3, TimeMode.AT, start = 720), monday, false),
        )
        assertEquals(listOf(2L, 1L, 3L), Schedule.sortForDay(items).map { it.task.id })
    }

    // --- Повторы ---

    @Test
    fun single_occursOnlyOnItsDay() {
        val t = task(1)
        assertTrue(Schedule.occursOn(t, monday))
        assertFalse(Schedule.occursOn(t, monday - 1))
        assertFalse(Schedule.occursOn(t, monday + 1))
    }

    @Test
    fun daily_occursFromStartDay() {
        val t = task(1, repeat = RepeatMode.DAILY)
        assertFalse(Schedule.occursOn(t, monday - 1))
        assertTrue(Schedule.occursOn(t, monday))
        assertTrue(Schedule.occursOn(t, monday + 1))
        assertTrue(Schedule.occursOn(t, monday + 365))
    }

    @Test
    fun daily_respectsEndDay() {
        val t = task(1, repeat = RepeatMode.DAILY, endDay = monday + 2)
        assertTrue(Schedule.occursOn(t, monday + 2))
        assertFalse(Schedule.occursOn(t, monday + 3))
    }

    @Test
    fun weekdays_onlySelectedDays() {
        // Понедельник и среда.
        val mask = Schedule.weekdayBit(1) or Schedule.weekdayBit(3)
        val t = task(1, repeat = RepeatMode.WEEKDAYS, mask = mask)
        assertTrue(Schedule.occursOn(t, monday))
        assertFalse(Schedule.occursOn(t, monday + 1))
        assertTrue(Schedule.occursOn(t, monday + 2))
        assertFalse(Schedule.occursOn(t, monday + 3))
        assertFalse(Schedule.occursOn(t, monday + 6))
        assertTrue(Schedule.occursOn(t, monday + 7))
        assertFalse(Schedule.occursOn(t, monday - 7))
    }

    @Test
    fun weekdays_startDayNotInMask_isNotShownOnStartDay() {
        // Создано в понедельник, повтор по пятницам.
        val t = task(1, repeat = RepeatMode.WEEKDAYS, mask = Schedule.weekdayBit(5))
        assertFalse(Schedule.occursOn(t, monday))
        assertTrue(Schedule.occursOn(t, monday + 4))
    }

    @Test
    fun skippedDay_isHidden() {
        val t = task(1, repeat = RepeatMode.DAILY)
        val skipped = setOf(DayKey(1, monday + 1))
        assertTrue(Schedule.occursOn(t, monday, skipped))
        assertFalse(Schedule.occursOn(t, monday + 1, skipped))
        assertTrue(Schedule.occursOn(t, monday + 2, skipped))
    }

    @Test
    fun occurrences_haveDoneFlagPerDay() {
        val data = PlannerData(
            tasks = listOf(task(1, repeat = RepeatMode.DAILY), task(2, day = monday + 1)),
            skipped = emptySet(),
            completions = setOf(DayKey(1, monday)),
        )
        val first = data.occurrencesOn(monday)
        assertEquals(1, first.size)
        assertTrue(first[0].done)
        val second = data.occurrencesOn(monday + 1)
        assertEquals(2, second.size)
        assertTrue(second.none { it.done })
    }

    // --- Время ближайшего сигнала ---

    @Test
    fun signal_singleTask_futureToday() {
        val t = task(1, TimeMode.AT, start = 600, signalStart = true)
        assertEquals(at(monday, 10, 0), Schedule.nextSignal(t, SignalKind.START, at(monday, 9, 0)))
    }

    @Test
    fun signal_singleTask_pastTime_isNotScheduled() {
        val t = task(1, TimeMode.AT, start = 600, signalStart = true)
        assertNull(Schedule.nextSignal(t, SignalKind.START, at(monday, 10, 0)))
        assertNull(Schedule.nextSignal(t, SignalKind.START, at(monday, 10, 1)))
        assertNull(Schedule.nextSignal(t, SignalKind.START, at(monday + 1, 9, 0)))
    }

    @Test
    fun signal_futureDay() {
        val t = task(1, TimeMode.AT, start = 600, day = monday + 3, signalStart = true)
        assertEquals(at(monday + 3, 10, 0), Schedule.nextSignal(t, SignalKind.START, at(monday, 12, 0)))
    }

    @Test
    fun signal_daily_movesToTomorrowAfterTime() {
        val t = task(1, TimeMode.AT, start = 600, repeat = RepeatMode.DAILY, signalStart = true)
        assertEquals(at(monday, 10, 0), Schedule.nextSignal(t, SignalKind.START, at(monday, 9, 59)))
        assertEquals(at(monday + 1, 10, 0), Schedule.nextSignal(t, SignalKind.START, at(monday, 11, 0)))
    }

    @Test
    fun signal_completedDay_isSkipped() {
        val t = task(1, TimeMode.AT, start = 600, repeat = RepeatMode.DAILY, signalStart = true)
        val done = setOf(DayKey(1, monday))
        assertEquals(
            at(monday + 1, 10, 0),
            Schedule.nextSignal(t, SignalKind.START, at(monday, 9, 0), completions = done),
        )
    }

    @Test
    fun signal_completedSingleTask_hasNoSignal() {
        val t = task(1, TimeMode.AT, start = 600, signalStart = true)
        val done = setOf(DayKey(1, monday))
        assertNull(Schedule.nextSignal(t, SignalKind.START, at(monday, 9, 0), completions = done))
    }

    @Test
    fun signal_weekdays_andSkippedDay() {
        val mask = Schedule.weekdayBit(1) or Schedule.weekdayBit(3)
        val t = task(1, TimeMode.AT, start = 600, repeat = RepeatMode.WEEKDAYS, mask = mask, signalStart = true)
        assertEquals(at(monday + 2, 10, 0), Schedule.nextSignal(t, SignalKind.START, at(monday, 11, 0)))
        val skipped = setOf(DayKey(1, monday + 2))
        assertEquals(
            at(monday + 7, 10, 0),
            Schedule.nextSignal(t, SignalKind.START, at(monday, 11, 0), skipped = skipped),
        )
    }

    @Test
    fun signal_respectsEndDay() {
        val t = task(1, TimeMode.AT, start = 600, repeat = RepeatMode.DAILY, endDay = monday, signalStart = true)
        assertNull(Schedule.nextSignal(t, SignalKind.START, at(monday, 11, 0)))
    }

    @Test
    fun signal_endOnlyForRange() {
        val range = task(1, TimeMode.RANGE, start = 600, end = 690, signalEnd = true)
        assertNull(Schedule.nextSignal(range, SignalKind.START, at(monday, 9, 0)))
        assertEquals(at(monday, 11, 30), Schedule.nextSignal(range, SignalKind.END, at(monday, 11, 0)))

        val point = task(2, TimeMode.AT, start = 600, signalEnd = true)
        assertNull(Schedule.nextSignal(point, SignalKind.END, at(monday, 9, 0)))
    }

    @Test
    fun signal_untimedTask_hasNoSignals() {
        val t = task(1, signalStart = true, signalEnd = true)
        assertNull(Schedule.nextSignal(t, SignalKind.START, at(monday, 9, 0)))
        assertNull(Schedule.nextSignal(t, SignalKind.END, at(monday, 9, 0)))
    }

    // --- Метки календаря ---

    @Test
    fun calendarMarks_includeRepeatingTasks() {
        val weekly = task(1, repeat = RepeatMode.WEEKDAYS, mask = Schedule.weekdayBit(1), endDay = monday + 14)
        val single = task(2, day = monday + 1 + 1)
        val data = PlannerData(listOf(weekly, single), emptySet(), emptySet())
        assertEquals(
            setOf(monday, monday + 2, monday + 7, monday + 14),
            data.markedDays(monday, monday + 20),
        )
        assertEquals(emptySet<Long>(), data.markedDays(monday - 7, monday - 1))
    }

    @Test
    fun calendarMarks_respectSkippedDays() {
        val weekly = task(1, repeat = RepeatMode.WEEKDAYS, mask = Schedule.weekdayBit(1), endDay = monday + 14)
        val data = PlannerData(listOf(weekly), setOf(DayKey(1, monday + 7)), emptySet())
        assertEquals(setOf(monday, monday + 14), data.markedDays(monday, monday + 20))
    }

    // --- Статистика ---

    @Test
    fun stats_countPlannedAndDone() {
        val daily = task(1, repeat = RepeatMode.DAILY)
        val single = task(2, day = monday + 1)
        val completions = setOf(
            DayKey(1, monday),
            DayKey(2, monday + 1),
            DayKey(1, monday + 5), // вне периода
            DayKey(2, monday), // отметка на день, где дела нет, не учитывается
        )
        val stat = PlannerData(listOf(daily, single), emptySet(), completions).stats(monday, monday + 2)
        assertEquals(3, stat.days.size)
        assertEquals(listOf(1, 2, 1), stat.days.map { it.planned })
        assertEquals(listOf(1, 1, 0), stat.days.map { it.done })
        assertEquals(4, stat.planned)
        assertEquals(2, stat.done)
        assertEquals(50, stat.percent)
    }

    @Test
    fun stats_emptyPeriod_isZeroPercent() {
        val stat = PlannerData.EMPTY.stats(monday, monday + 6)
        assertEquals(0, stat.planned)
        assertEquals(0, stat.done)
        assertEquals(0, stat.percent)
        assertEquals(7, stat.days.size)
    }

    // --- Подпись времени ---

    @Test
    fun timeLabel_is24Hour() {
        assertEquals("", TimeText.label(task(1)))
        assertEquals("09:05", TimeText.label(task(1, TimeMode.AT, start = 545)))
        assertEquals("13:00–23:59", TimeText.label(task(1, TimeMode.RANGE, start = 780, end = 1439)))
    }
}
