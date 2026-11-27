package ru.dayplanner.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.ui.components.SectionCard

@Composable
fun TemplatePickButton(state: EditorState, onChange: (EditorState) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) { Text(stringResource(R.string.pick_template)) }
    if (!open) return
    val templates by LocalContext.current.container.repository.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(R.string.pick_template)) }, text = {
        if (templates.isEmpty()) Text(stringResource(R.string.no_templates)) else LazyColumn { items(templates, key={it.id}) { t ->
            TextButton(onClick={ onChange(state.copy(title=t.title, description=t.description, signalStart=t.signalStart, signalEnd=t.signalEnd, signalType=t.signalType)); open=false }) { Text(t.title) }
        } }
    }, confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } })
}

@Composable
fun TemplateSection(state: EditorState, onChange: (EditorState) -> Unit) {
    SectionCard { Row(verticalAlignment=Alignment.CenterVertically) { Text(stringResource(R.string.save_template), Modifier.weight(1f)); Switch(checked=state.saveAsTemplate, onCheckedChange={ onChange(state.copy(saveAsTemplate=it)) }) } }
}
