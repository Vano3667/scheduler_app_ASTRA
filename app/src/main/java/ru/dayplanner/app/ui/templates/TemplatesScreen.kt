package ru.dayplanner.app.ui.templates

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.*
import ru.dayplanner.app.ui.components.*

@Composable
fun TemplatesScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val c = LocalContext.current.container
    val list by c.repository.templates.collectAsStateWithLifecycle(initialValue=emptyList())
    var remove by rememberSaveable { mutableStateOf<Long?>(null) }
    ScreenScaffold(stringResource(R.string.open_templates), onBack, floatingActionButton={ FloatingActionButton(onClick={onEdit(0L)}) { Icon(Icons.Rounded.Add, stringResource(R.string.new_template)) } }) { p ->
        if(list.isEmpty()) EmptyState(Icons.Rounded.Bookmarks,stringResource(R.string.no_templates),stringResource(R.string.templates_hint), Modifier.padding(p))
        else LazyColumn(Modifier.padding(p), contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=90.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) { items(list, key={it.id}) { t ->
            SectionCard { Row(verticalAlignment=Alignment.CenterVertically) { TextButton(onClick={onEdit(t.id)},modifier=Modifier.weight(1f)) { Text(t.title) }; IconButton(onClick={remove=t.id}) { Icon(Icons.Rounded.DeleteOutline,stringResource(R.string.delete)) } }; if(t.description.isNotBlank()) Text(t.description,maxLines=2) }
        } }
    }
    remove?.let { id -> ConfirmDialog(stringResource(R.string.delete),stringResource(R.string.remove_template),stringResource(R.string.delete), {remove=null;c.launch { c.repository.deleteTemplate(id) }},{remove=null}) }
}

@Composable
fun TemplateEditorScreen(id: Long, onBack: () -> Unit) {
    val c=LocalContext.current.container
    var title by rememberSaveable(id){ mutableStateOf("") }; var desc by rememberSaveable(id){ mutableStateOf("") }
    var start by rememberSaveable(id){ mutableStateOf(false) }; var end by rememberSaveable(id){ mutableStateOf(false) }; var alarm by rememberSaveable(id){ mutableStateOf(false) }
    var ready by rememberSaveable(id){ mutableStateOf(id==0L) }; var busy by remember { mutableStateOf(false) }
    LaunchedEffect(id){ if(!ready){ val t=c.repository.getTemplate(id); if(t==null){onBack();return@LaunchedEffect};title=t.title;desc=t.description;start=t.signalStart;end=t.signalEnd;alarm=t.signalType==SignalType.ALARM;ready=true } }
    ScreenScaffold(stringResource(if(id==0L) R.string.new_template else R.string.edit_template),onBack){ p ->
        if(ready) Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedTextField(title,{title=it},label={Text(stringResource(R.string.field_title))},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(desc,{desc=it},label={Text(stringResource(R.string.field_description))},modifier=Modifier.fillMaxWidth());SectionCard(title=stringResource(R.string.template_signals)){
                SignalSwitch(R.string.signal_when_start,start){start=it}; SignalSwitch(R.string.signal_when_end,end){end=it}; SignalSwitch(R.string.signal_type_alarm,alarm){alarm=it}
            };Button(enabled=title.isNotBlank()&&!busy,onClick={busy=true;c.launch { try { c.repository.saveTemplate(Template(id,title.trim(),desc.trim(),start,end,if(alarm) SignalType.ALARM else SignalType.NOTIFICATION));onBack() } finally {busy=false} }}){Text(stringResource(R.string.save))}
        }
    }
}

@Composable
fun SignalSwitch(label:Int,checked:Boolean,onChange:(Boolean)->Unit) { Row(verticalAlignment=Alignment.CenterVertically) { Text(stringResource(label),Modifier.weight(1f));Switch(checked=checked,onCheckedChange=onChange) } }
