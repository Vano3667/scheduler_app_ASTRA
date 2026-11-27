package ru.dayplanner.app.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import ru.dayplanner.app.ui.theme.AppStyle

// Эффекты стиля «С эффектами» появятся на этапе 9. Пока это пустая заглушка с тем же интерфейсом.

interface EffectsController {
    /** Дело отмечено выполненным; [allDone] — выполнены все дела дня. */
    fun onTaskCompleted(allDone: Boolean)
}

object NoEffects : EffectsController {
    override fun onTaskCompleted(allDone: Boolean) {}
}

val LocalEffects = staticCompositionLocalOf<EffectsController> { NoEffects }

@Composable
fun EffectsHost(style: AppStyle, content: @Composable () -> Unit) {
    content()
}

/** Вспышка с частицами при нажатии (только в стиле «С эффектами»). */
fun Modifier.pressBurst(): Modifier = this
