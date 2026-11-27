package ru.dayplanner.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.dayplanner.app.R
import ru.dayplanner.app.ui.components.ScreenScaffold
import ru.dayplanner.app.ui.components.SectionCard

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.open_settings), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StyleSection()
            PermissionsSection()
            BackupSection()
            AboutSection()
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    SectionCard(title = stringResource(R.string.about_title)) {
        Text(
            text = stringResource(R.string.app_name) + " · " + stringResource(R.string.about_version, version),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.about_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
