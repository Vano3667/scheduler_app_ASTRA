package ru.dayplanner.app.ui.stats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.*
import ru.dayplanner.app.ui.*
import ru.dayplanner.app.ui.components.*
import java.time.*

@Composable
fun StatsScreen(onBack:()->Unit){val today=LocalDate.ofEpochDay(rememberToday());var monthly by rememberSaveable{mutableStateOf(false)};var offset by rememberSaveable{mutableIntStateOf(0)};var details by rememberSaveable{mutableStateOf(false)};val first=if(monthly) today.withDayOfMonth(1).plusMonths(offset.toLong()) else today.minusDays(today.dayOfWeek.value-1L).plusWeeks(offset.toLong());val last=if(monthly)first.plusMonths(1).minusDays(1) else first.plusDays(6);val period="${Fmt.fullDate(first.toEpochDay())} — ${Fmt.fullDate(last.toEpochDay())}";val snap by LocalContext.current.container.repository.data.collectAsStateWithLifecycle();val stat=remember(snap,first,last){snap?.stats(first.toEpochDay(),last.toEpochDay())}
    ScreenScaffold(stringResource(R.string.open_stats),onBack){p->Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(!monthly,{monthly=false;offset=0},{Text(stringResource(R.string.stats_week))});FilterChip(monthly,{monthly=true;offset=0},{Text(stringResource(R.string.stats_month))})}
        Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={offset--},enabled=first.year>=1970){Icon(Icons.Rounded.ChevronLeft,stringResource(R.string.stats_prev))}; Text(period,Modifier.weight(1f));IconButton(onClick={offset++},enabled=offset<0){Icon(Icons.Rounded.ChevronRight,stringResource(R.string.stats_next))}}
        if(stat==null)LinearProgressIndicator() else {SectionCard{Text(stringResource(R.string.stats_percent,stat.percent),style=MaterialTheme.typography.headlineMedium);Text(stringResource(R.string.stats_totals,stat.planned,stat.done))};SectionCard(title=stringResource(R.string.stats_by_day)){Text(stringResource(R.string.stats_source,period),style=MaterialTheme.typography.bodySmall);DayBars(stat);TextButton(onClick={details=!details}){Text(stringResource(R.string.stats_details))};if(details)stat.days.forEach{Text(stringResource(R.string.stats_day,Fmt.shortDate(it.day),it.done,it.planned))}}}
    }}
}
@Composable
private fun DayBars(stat:PeriodStat){val dark=isSystemInDarkTheme();val planned=if(dark)Color(0xff9b82ff) else Color(0xff7b5cff);val done=if(dark)Color(0xff34c98e) else Color(0xff1a9e6e);val max=stat.days.maxOfOrNull{it.planned}?.coerceAtLeast(1)?:1
    FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(planned to R.string.stats_planned,done to R.string.stats_done).forEach{(color, label)->Row(verticalAlignment=Alignment.CenterVertically){Spacer(Modifier.size(10.dp).background(color));Spacer(Modifier.width(4.dp));Text(stringResource(label),style=MaterialTheme.typography.labelMedium)}}}
    Text("0 – $max",style=MaterialTheme.typography.labelSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){stat.days.forEach{d->val desc=stringResource(R.string.stats_day,Fmt.shortDate(d.day),d.done,d.planned);Column(Modifier.width(30.dp).semantics {contentDescription=desc},horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.height(100.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(2.dp)){listOf(planned to d.planned,done to d.done).forEach{(color,count)->Spacer(Modifier.width(10.dp).height((100f*count/max).dp).background(color,RoundedCornerShape(topStart=3.dp,topEnd=3.dp)))}};Text(LocalDate.ofEpochDay(d.day).dayOfMonth.toString(),style=MaterialTheme.typography.labelSmall)} } }
}
