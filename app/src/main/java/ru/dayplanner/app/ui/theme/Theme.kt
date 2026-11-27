package ru.dayplanner.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.container

enum class AppStyle { NORMAL, EFFECTS }
val LocalAppStyle = staticCompositionLocalOf { AppStyle.NORMAL }
val LocalPlannerDark = staticCompositionLocalOf { false }
private val base = Typography()
val PlannerTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
val PlannerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)
val TimeTextStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum", letterSpacing = 0.2.sp)

private tailrec fun Context.activityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activityOrNull()
    else -> null
}

@Composable
fun PlannerTheme(style: AppStyle, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val mode by context.container.settings.themeMode.collectAsStateWithLifecycle()
    val dark = mode.isDark(isSystemInDarkTheme())
    val scheme = when (style) {
        AppStyle.NORMAL -> if (dark) NormalDark else NormalLight
        AppStyle.EFFECTS -> if (dark) EffectsDark else EffectsLight
    }
    val view = LocalView.current
    SideEffect {
        context.activityOrNull()?.window?.let { window ->
            val bar = WindowCompat.getInsetsController(window, view)
            bar.isAppearanceLightStatusBars = !dark
            bar.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalAppStyle provides style, LocalPlannerDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = PlannerTypography, shapes = PlannerShapes, content = content)
    }
}
