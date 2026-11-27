package ru.dayplanner.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.container
import ru.dayplanner.app.ui.effects.EffectsHost
import ru.dayplanner.app.ui.theme.PlannerTheme

@Composable
fun PlannerRoot() {
    val container = LocalContext.current.container
    val style by container.settings.style.collectAsStateWithLifecycle()
    PlannerTheme(style = style) {
        EffectsHost(style = style) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                PlannerNavHost()
            }
        }
    }
}
