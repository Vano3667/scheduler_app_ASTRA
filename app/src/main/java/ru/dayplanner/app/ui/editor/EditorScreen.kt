package ru.dayplanner.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.EditScope
import ru.dayplanner.app.domain.TimeMode
import ru.dayplanner.app.domain.TimeText
import ru.dayplanner.app.ui.Fmt
import ru.dayplanner.app.ui.components.ScopeDialog
import ru.dayplanner.app.ui.components.SectionCard
import ru.dayplanner.app.ui.components.TimePickerDialog
import ru.dayplanner.app.ui.components.plannerTopBarColors
import ru.dayplanner.app.ui.effects.pressBurst

/** Добавление и редактирование дела. taskId = 0 — новое дело на день [day]. */
@Composable
fun EditorScreen(taskId: Long, day: Long, onClose: () -> Unit) {
    val container = LocalContext.current.container
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(container, taskId, day))
    val state = vm.state
    var askScope by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    LaunchedEffect(vm.missing) {
        if (vm.missing) onClose()
    }

    val trySave: () -> Unit = {
        if (vm.validate()) {
            if (vm.needsScopeQuestion) askScope = true else vm.save(EditScope.ALL, onClose)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (vm.isNew) R.string.editor_new else R.string.editor_edit),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    TextButton(onClick = trySave, enabled = vm.loaded) {
                        Text(stringResource(R.string.save))
                    }
                },
                colors = plannerTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = Fmt.weekday(day) + ", " + Fmt.fullDate(day),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TemplatePickButton(state = state, onChange = vm::update)

            OutlinedTextField(
                value = state.title,
                onValueChange = { vm.update(vm.state.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_title)) },
                isError = state.titleError,
                supportingText = if (state.titleError) {
                    { Text(stringResource(R.string.field_title_error)) }
                } else {
                    null
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                shape = MaterialTheme.shapes.medium,
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { vm.update(vm.state.copy(description = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_description)) },
                minLines = 2,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
            )

            SectionCard(title = stringResource(R.string.time_section)) {
                val modes = listOf(
                    TimeMode.NONE to R.string.time_none,
                    TimeMode.AT to R.string.time_at,
                    TimeMode.RANGE to R.string.time_range,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = state.timeMode == item.first,
                            onClick = { vm.update(vm.state.copy(timeMode = item.first)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            icon = {},
                        ) {
                            Text(text = stringResource(item.second), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                AnimatedVisibility(visible = state.timeMode != TimeMode.NONE) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeChip(
                                label = stringResource(
                                    if (state.timeMode == TimeMode.RANGE) R.string.time_start else R.string.time_single,
                                ),
                                minutes = state.startMinutes,
                                modifier = Modifier.weight(1f),
                                onClick = { pickStart = true },
                            )
                            if (state.timeMode == TimeMode.RANGE) {
                                TimeChip(
                                    label = stringResource(R.string.time_end),
                                    minutes = state.endMinutes,
                                    modifier = Modifier.weight(1f),
                                    onClick = { pickEnd = true },
                                )
                            }
                        }
                        if (state.timeError) {
                            Text(
                                text = stringResource(R.string.time_range_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            SignalSection(state = state, onChange = vm::update)
            RepeatSection(state = state, onChange = vm::update, day = day)
            PhotoSection(photos = state.photos, onChange = { vm.update(vm.state.copy(photos = it)) })
            TemplateSection(state = state, onChange = vm::update)

            Button(
                onClick = trySave,
                enabled = vm.loaded,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressBurst(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (pickStart) {
        TimePickerDialog(
            title = stringResource(R.string.pick_time),
            initialMinutes = state.startMinutes,
            onDismiss = { pickStart = false },
            onConfirm = {
                pickStart = false
                vm.update(vm.state.copy(startMinutes = it))
            },
        )
    }
    if (pickEnd) {
        TimePickerDialog(
            title = stringResource(R.string.pick_time),
            initialMinutes = state.endMinutes,
            onDismiss = { pickEnd = false },
            onConfirm = {
                pickEnd = false
                vm.update(vm.state.copy(endMinutes = it))
            },
        )
    }
    if (askScope) {
        ScopeDialog(
            title = stringResource(R.string.scope_edit_title),
            onDismiss = { askScope = false },
            onPick = { scope ->
                askScope = false
                vm.save(scope, onClose)
            },
        )
    }
}

@Composable
private fun TimeChip(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = TimeText.minutes(minutes), style = MaterialTheme.typography.headlineSmall)
        }
    }
}
