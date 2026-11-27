package ru.dayplanner.app.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.dayplanner.app.R
import ru.dayplanner.app.ui.components.PlaceholderScreen

// Статистика появится на этапе 7.

@Composable
fun StatsScreen(onBack: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.open_stats), onBack = onBack)
}
