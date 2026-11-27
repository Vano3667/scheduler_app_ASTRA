package ru.dayplanner.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.dp
import ru.dayplanner.app.R
import ru.dayplanner.app.domain.*
import ru.dayplanner.app.ui.components.SectionCard

@Composable
fun RepeatSection(state: EditorState, onChange: (EditorState) -> Unit, day: Long) {
    SectionCard(title = stringResource(R.string.repeat_section)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(RepeatMode.NONE to R.string.repeat_no, RepeatMode.DAILY to R.string.repeat_daily, RepeatMode.WEEKDAYS to R.string.repeat_days).forEach { (mode,label) ->
                FilterChip(selected = state.repeatMode == mode, onClick = { onChange(state.copy(repeatMode = mode, weekdaysMask = if(mode == RepeatMode.WEEKDAYS && state.weekdaysMask == 0) Schedule.weekdayBit(Schedule.dayOfWeek(day)) else state.weekdaysMask)) }, label = { Text(stringResource(label)) })
            }
        }
        if (state.repeatMode == RepeatMode.WEEKDAYS) {
            val names = stringArrayResource(R.array.weekdays_short)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { names.forEachIndexed { i, label ->
                val bit = 1 shl i
                FilterChip(selected = state.weekdaysMask and bit != 0, onClick = { onChange(state.copy(weekdaysMask = state.weekdaysMask xor bit)) }, label = { Text(label) })
            } }
            if (state.weekdaysError) Text(stringResource(R.string.repeat_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
