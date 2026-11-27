package ru.dayplanner.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import ru.dayplanner.app.R
import ru.dayplanner.app.domain.RepeatMode
import ru.dayplanner.app.domain.SignalType
import ru.dayplanner.app.domain.Task
import ru.dayplanner.app.domain.TimeMode

/** Описание повтора дела словами или null, если дело не повторяется. */
@Composable
fun repeatText(task: Task): String? = when (task.repeatMode) {
    RepeatMode.NONE -> null
    RepeatMode.DAILY -> stringResource(R.string.repeat_daily)
    RepeatMode.WEEKDAYS -> {
        val names = stringArrayResource(R.array.weekdays_short)
        val days = (0..6).filter { (task.weekdaysMask and (1 shl it)) != 0 }.joinToString(", ") { names[it] }
        stringResource(R.string.repeat_on_days, days)
    }
}

/** Описание сигналов дела словами или null, если сигналов нет. */
@Composable
fun signalText(task: Task): String? {
    if (task.timeMode == TimeMode.NONE) return null
    val start = task.signalStart
    val end = task.signalEnd && task.timeMode == TimeMode.RANGE
    if (!start && !end) return null
    val whenRes = when {
        start && end -> R.string.signal_when_both
        start -> R.string.signal_when_start
        else -> R.string.signal_when_end
    }
    val typeRes = if (task.signalType == SignalType.ALARM) {
        R.string.signal_type_alarm
    } else {
        R.string.signal_type_notification
    }
    return stringResource(R.string.signal_summary, stringResource(whenRes), stringResource(typeRes))
}
