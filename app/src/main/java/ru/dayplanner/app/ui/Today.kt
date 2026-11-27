package ru.dayplanner.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Сегодняшний день (номер дня от 1970-01-01); обновляется при смене суток и при возврате в приложение. */
@Composable
fun rememberToday(): Long {
    var today by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today = LocalDate.now().toEpochDay()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            today = LocalDate.now().toEpochDay()
        }
    }
    return today
}
