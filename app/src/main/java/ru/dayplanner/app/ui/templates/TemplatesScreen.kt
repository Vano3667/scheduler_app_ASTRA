package ru.dayplanner.app.ui.templates

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.dayplanner.app.R
import ru.dayplanner.app.ui.components.PlaceholderScreen

// Типовые дела появятся на этапе 4.

@Composable
fun TemplatesScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.open_templates), onBack = onBack)
}

@Composable
fun TemplateEditorScreen(id: Long, onBack: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.open_templates), onBack = onBack)
}
