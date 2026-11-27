package ru.dayplanner.app.ui.editor

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.dayplanner.app.R
import ru.dayplanner.app.domain.*
import ru.dayplanner.app.ui.components.SectionCard
import ru.dayplanner.app.ui.templates.SignalSwitch
import ru.dayplanner.app.ui.settings.PermissionsSection

@Composable
fun SignalSection(state: EditorState, onChange: (EditorState) -> Unit) {
    if(state.timeMode==TimeMode.NONE)return
    SectionCard(title=stringResource(R.string.template_signals)) {
        SignalSwitch(R.string.signal_when_start,state.signalStart){onChange(state.copy(signalStart=it))}
        if(state.timeMode==TimeMode.RANGE) SignalSwitch(R.string.signal_when_end,state.signalEnd){onChange(state.copy(signalEnd=it))}
        if(state.signalStart||(state.timeMode==TimeMode.RANGE&&state.signalEnd)){
            val alarm=state.signalType==SignalType.ALARM
            SignalSwitch(R.string.signal_type_alarm,alarm){onChange(state.copy(signalType=if(it)SignalType.ALARM else SignalType.NOTIFICATION))};PermissionsSection(alarm)
        }
    }
}
