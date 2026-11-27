package ru.dayplanner.app.ui.day

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.dayplanner.app.PlannerApp
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.Occurrence
import ru.dayplanner.app.domain.SignalType
import ru.dayplanner.app.domain.TimeMode
import ru.dayplanner.app.domain.TimeText
import ru.dayplanner.app.ui.Fmt
import ru.dayplanner.app.ui.components.AnimatedCheck
import ru.dayplanner.app.ui.components.EmptyState
import ru.dayplanner.app.ui.components.plannerTopBarColors
import ru.dayplanner.app.ui.effects.LocalEffects
import ru.dayplanner.app.ui.effects.pressBurst
import ru.dayplanner.app.ui.rememberToday
import ru.dayplanner.app.ui.theme.TimeTextStyle
import java.time.LocalDate

/** Номер страницы листалки равен номеру дня от 1970-01-01. */
const val DAY_PAGE_COUNT = 80_000

@Composable
fun DayScreen(
    pendingDay: Long?,
    onPendingDayConsumed: () -> Unit,
    onOpenCalendar: (Long) -> Unit,
    onAdd: (Long) -> Unit,
    onOpenTask: (Long, Long) -> Unit,
    onTemplates: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val today = rememberToday()
    val pagerState = rememberPagerState(initialPage = today.toInt()) { DAY_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val currentDay = pagerState.currentPage.toLong()

    // При запуске приложения всегда открывается сегодняшний день.
    LaunchedEffect(Unit) {
        val app = context.applicationContext as PlannerApp
        if (app.consumeFreshStart()) {
            pagerState.scrollToPage(LocalDate.now().toEpochDay().toInt())
        }
    }
    // Переход к дню, выбранному в календаре.
    LaunchedEffect(pendingDay) {
        if (pendingDay != null) {
            pagerState.scrollToPage(pendingDay.toInt().coerceIn(0, DAY_PAGE_COUNT - 1))
            onPendingDayConsumed()
        }
    }
    // Смена суток: если был открыт «сегодня», переходим на новый «сегодня».
    var knownToday by rememberSaveable { mutableStateOf(today) }
    LaunchedEffect(today) {
        if (today != knownToday) {
            if (pagerState.currentPage.toLong() == knownToday) pagerState.scrollToPage(today.toInt())
            knownToday = today
        }
    }

    Scaffold(
        topBar = {
            DayTopBar(
                day = currentDay,
                today = today,
                onCalendar = { onOpenCalendar(currentDay) },
                onTemplates = onTemplates,
                onStats = onStats,
                onSettings = onSettings,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAdd(currentDay) },
                modifier = Modifier.pressBurst(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
            ) { page ->
                DayPage(day = page.toLong(), onOpenTask = onOpenTask)
            }
            AnimatedVisibility(
                visible = currentDay != today,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                FilledTonalButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(today.toInt()) } },
                    modifier = Modifier.pressBurst(),
                ) {
                    Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.today))
                }
            }
        }
    }
}

@Composable
private fun DayTopBar(
    day: Long,
    today: Long,
    onCalendar: () -> Unit,
    onTemplates: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = day,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "dayTitle",
            ) { shown ->
                val isToday = shown == today
                Column {
                    Text(
                        text = Fmt.dayTitle(shown, today),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isToday) {
                            Fmt.weekday(shown) + " · " + stringResource(R.string.today_suffix)
                        } else {
                            Fmt.weekday(shown)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onCalendar, modifier = Modifier.pressBurst()) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.open_calendar))
            }
            IconButton(onClick = onTemplates, modifier = Modifier.pressBurst()) {
                Icon(Icons.Rounded.Bookmarks, contentDescription = stringResource(R.string.open_templates))
            }
            IconButton(onClick = onStats, modifier = Modifier.pressBurst()) {
                Icon(Icons.Rounded.BarChart, contentDescription = stringResource(R.string.open_stats))
            }
            IconButton(onClick = onSettings, modifier = Modifier.pressBurst()) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.open_settings))
            }
        },
        colors = plannerTopBarColors(),
    )
}

@Composable
private fun DayPage(day: Long, onOpenTask: (Long, Long) -> Unit) {
    val container = LocalContext.current.container
    val data by container.repository.data.collectAsStateWithLifecycle()
    val effects = LocalEffects.current
    val items = remember(data, day) { data?.occurrencesOn(day) }

    if (items == null) {
        Box(Modifier.fillMaxSize())
    } else if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.EventAvailable,
            title = stringResource(R.string.empty_day_title),
            hint = stringResource(R.string.empty_day_hint),
        )
    } else {
        val untimed = items.filter { it.task.timeMode == TimeMode.NONE }
        val timed = items.filter { it.task.timeMode != TimeMode.NONE }
        val onToggle: (Occurrence, Boolean) -> Unit = { occ, checked ->
            if (checked) {
                val allDone = items.all { it.done || it.task.id == occ.task.id }
                effects.onTaskCompleted(allDone)
            }
            container.launch { container.repository.setDone(occ.task.id, day, checked) }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (untimed.isNotEmpty()) {
                item(key = "header_untimed") {
                    SectionHeader(stringResource(R.string.section_all_day), Modifier.animateItem())
                }
                items(untimed, key = { it.task.id }) { occ ->
                    TaskRow(
                        occ = occ,
                        modifier = Modifier.animateItem(),
                        onToggle = { onToggle(occ, it) },
                        onClick = { onOpenTask(occ.task.id, day) },
                    )
                }
            }
            if (timed.isNotEmpty()) {
                item(key = "header_timed") {
                    SectionHeader(stringResource(R.string.section_timed), Modifier.animateItem())
                }
                items(timed, key = { it.task.id }) { occ ->
                    TaskRow(
                        occ = occ,
                        modifier = Modifier.animateItem(),
                        onToggle = { onToggle(occ, it) },
                        onClick = { onOpenTask(occ.task.id, day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Строка дела: галочка, время, название. */
@Composable
private fun TaskRow(
    occ: Occurrence,
    modifier: Modifier,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val task = occ.task
    val alpha by animateFloatAsState(targetValue = if (occ.done) 0.5f else 1f, label = "rowAlpha")
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedCheck(checked = occ.done, onCheckedChange = onToggle)
            Spacer(Modifier.width(2.dp))
            if (task.timeMode != TimeMode.NONE) {
                Text(
                    text = TimeText.label(task),
                    style = TimeTextStyle,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                textDecoration = if (occ.done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (task.isRepeating) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
            if (task.timeMode != TimeMode.NONE && (task.signalStart || task.signalEnd)) {
                Icon(
                    imageVector = if (task.signalType == SignalType.ALARM) {
                        Icons.Rounded.Alarm
                    } else {
                        Icons.Rounded.NotificationsActive
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
        }
    }
}
