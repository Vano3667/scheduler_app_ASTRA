package ru.dayplanner.app.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.DayKey
import ru.dayplanner.app.domain.EditScope
import ru.dayplanner.app.domain.Schedule
import ru.dayplanner.app.domain.SignalType
import ru.dayplanner.app.domain.TimeMode
import ru.dayplanner.app.domain.TimeText
import ru.dayplanner.app.ui.Fmt
import ru.dayplanner.app.ui.components.ConfirmDialog
import ru.dayplanner.app.ui.components.InfoRow
import ru.dayplanner.app.ui.components.ScopeDialog
import ru.dayplanner.app.ui.components.ScreenScaffold
import ru.dayplanner.app.ui.components.SectionCard
import ru.dayplanner.app.ui.effects.LocalEffects
import ru.dayplanner.app.ui.effects.pressBurst
import ru.dayplanner.app.ui.repeatText
import ru.dayplanner.app.ui.signalText

/** Страница дела в конкретный день. */
@Composable
fun TaskScreen(
    taskId: Long,
    day: Long,
    onBack: () -> Unit,
    onGone: () -> Unit,
    onEdit: () -> Unit,
) {
    val container = LocalContext.current.container
    val data by container.repository.data.collectAsStateWithLifecycle()
    val effects = LocalEffects.current
    val snapshot = data
    val task = snapshot?.tasks?.firstOrNull { it.id == taskId }
    // Дело удалено или больше не относится к этому дню — закрываем страницу.
    val gone = snapshot != null && (task == null || !Schedule.occursOn(task, day, snapshot.skipped))
    LaunchedEffect(gone) {
        if (gone) onGone()
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var askScope by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.task_title), onBack = onBack) { padding ->
        if (snapshot == null || task == null || gone) {
            Box(Modifier.fillMaxSize())
        } else {
            val done = DayKey(taskId, day) in snapshot.completions
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionCard {
                    Text(
                        text = if (task.timeMode == TimeMode.NONE) {
                            stringResource(R.string.section_all_day)
                        } else {
                            TimeText.label(task)
                        },
                        style = if (task.timeMode == TimeMode.NONE) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.displaySmall
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    InfoRow(icon = Icons.Rounded.Event, text = Fmt.weekday(day) + ", " + Fmt.fullDate(day))
                    val repeat = repeatText(task)
                    if (repeat != null) InfoRow(icon = Icons.Rounded.Repeat, text = repeat)
                    val signal = signalText(task)
                    if (signal != null) {
                        InfoRow(
                            icon = if (task.signalType == SignalType.ALARM) {
                                Icons.Rounded.Alarm
                            } else {
                                Icons.Rounded.NotificationsActive
                            },
                            text = signal,
                        )
                    }
                }

                if (task.description.isNotBlank()) {
                    SectionCard(title = stringResource(R.string.field_description)) {
                        Text(text = task.description, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                TaskPhotos(taskId = taskId)

                if (done) {
                    FilledTonalButton(
                        onClick = { container.launch { container.repository.setDone(taskId, day, false) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.done_state) + " · " + stringResource(R.string.done_unmark))
                    }
                } else {
                    Button(
                        onClick = {
                            val allDone = snapshot.occurrencesOn(day).all { it.done || it.task.id == taskId }
                            effects.onTaskCompleted(allDone)
                            container.launch { container.repository.setDone(taskId, day, true) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressBurst(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.done_mark))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .pressBurst(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(R.string.edit), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { if (task.isRepeating) askScope = true else confirmDelete = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(R.string.delete), maxLines = 1)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (confirmDelete && task != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            text = stringResource(R.string.delete_confirm_text, task.title),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                container.launch { container.repository.deleteTask(taskId, day, EditScope.ALL) }
            },
            onDismiss = { confirmDelete = false },
        )
    }
    if (askScope) {
        ScopeDialog(
            title = stringResource(R.string.scope_delete_title),
            onDismiss = { askScope = false },
            onPick = { scope ->
                askScope = false
                container.launch { container.repository.deleteTask(taskId, day, scope) }
            },
        )
    }
}
