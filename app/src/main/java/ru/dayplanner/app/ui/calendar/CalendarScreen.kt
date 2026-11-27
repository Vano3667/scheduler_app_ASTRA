package ru.dayplanner.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.PlannerData
import ru.dayplanner.app.ui.Fmt
import ru.dayplanner.app.ui.components.BackButton
import ru.dayplanner.app.ui.components.plannerTopBarColors
import ru.dayplanner.app.ui.rememberToday
import java.time.LocalDate
import java.time.YearMonth

/** Номер страницы = год * 12 + (месяц - 1). */
private const val MONTH_PAGES = 2200 * 12

@Composable
fun CalendarScreen(initialDay: Long, onBack: () -> Unit, onPickDay: (Long) -> Unit) {
    val container = LocalContext.current.container
    val data by container.repository.data.collectAsStateWithLifecycle()
    val today = rememberToday()
    val initialPage = remember(initialDay) {
        val date = LocalDate.ofEpochDay(initialDay)
        (date.year * 12 + date.monthValue - 1).coerceIn(0, MONTH_PAGES - 1)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { MONTH_PAGES }
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val month = YearMonth.of(page / 12, page % 12 + 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Fmt.monthTitle(month), maxLines = 1) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage((page - 1).coerceAtLeast(0)) }
                        },
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.prev_month))
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage((page + 1).coerceAtMost(MONTH_PAGES - 1))
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.next_month))
                    }
                },
                colors = plannerTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            WeekdayHeader()
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top,
                key = { it },
            ) { p ->
                MonthGrid(
                    month = YearMonth.of(p / 12, p % 12 + 1),
                    today = today,
                    selectedDay = initialDay,
                    data = data,
                    onPickDay = onPickDay,
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val names = stringArrayResource(R.array.weekdays_short)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        names.forEachIndexed { index, name ->
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = if (index >= 5) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: Long,
    selectedDay: Long,
    data: PlannerData?,
    onPickDay: (Long) -> Unit,
) {
    val firstDay = month.atDay(1).toEpochDay()
    val length = month.lengthOfMonth()
    val offset = month.atDay(1).dayOfWeek.value - 1
    val marked = remember(data, firstDay) {
        data?.markedDays(firstDay, firstDay + length - 1) ?: emptySet()
    }
    val rows = (offset + length + 6) / 7
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col - offset
                    if (index < 0 || index >= length) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                        )
                    } else {
                        val day = firstDay + index
                        DayCell(
                            number = index + 1,
                            isToday = day == today,
                            isSelected = day == selectedDay,
                            marked = day in marked,
                            onClick = { onPickDay(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.calendar_legend),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DayCell(
    number: Int,
    isToday: Boolean,
    isSelected: Boolean,
    marked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        isToday -> scheme.primary
        isSelected -> scheme.primaryContainer
        else -> Color.Transparent
    }
    val foreground = when {
        isToday -> scheme.onPrimary
        isSelected -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    val dot = when {
        !marked -> Color.Transparent
        isToday -> scheme.onPrimary
        else -> scheme.primary
    }
    Box(
        modifier = modifier
            .height(56.dp)
            .padding(3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = foreground,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            )
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
        }
    }
}
