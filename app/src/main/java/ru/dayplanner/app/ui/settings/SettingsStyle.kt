package ru.dayplanner.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.ui.components.SectionCard
import ru.dayplanner.app.ui.theme.AppStyle
import ru.dayplanner.app.ui.theme.ThemeMode

@Composable
fun StyleSection() {
    val store = LocalContext.current.container.settings
    val mode by store.themeMode.collectAsStateWithLifecycle()
    val style by store.style.collectAsStateWithLifecycle()
    SectionCard(title = stringResource(R.string.astra_theme_title)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.SYSTEM to R.string.astra_theme_system, ThemeMode.LIGHT to R.string.astra_theme_light, ThemeMode.DARK to R.string.astra_theme_dark).forEach { (choice, label) ->
                FilterChip(selected = mode == choice, onClick = { store.setThemeMode(choice) }, label = { Text(stringResource(label)) })
            }
        }
    }
    SectionCard(title = stringResource(R.string.astra_style)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(AppStyle.NORMAL to R.string.style_normal, AppStyle.EFFECTS to R.string.style_fx).forEach { (choice, label) ->
                FilterChip(selected = style == choice, onClick = { store.setStyle(choice) }, label = { Text(stringResource(label)) })
            }
        }
    }
}
