package ru.dayplanner.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.dayplanner.app.ui.theme.AppStyle
import ru.dayplanner.app.ui.theme.ThemeMode
import ru.dayplanner.app.widget.WidgetUpdater

class SettingsStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val styleState = MutableStateFlow(AppStyle.entries.firstOrNull { it.name == prefs.getString(KEY_STYLE, null) } ?: AppStyle.NORMAL)
    val style: StateFlow<AppStyle> = styleState
    private val modeState = MutableStateFlow(ThemeMode.fromPreference(prefs.getString(KEY_MODE, null)))
    val themeMode: StateFlow<ThemeMode> = modeState

    fun setStyle(style: AppStyle) {
        prefs.edit().putString(KEY_STYLE, style.name).apply()
        styleState.value = style
    }

    fun setThemeMode(mode: ThemeMode) {
        if (modeState.value == mode) return
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        modeState.value = mode
        WidgetUpdater.requestUpdate(app)
    }

    private companion object {
        const val KEY_STYLE = "style"
        const val KEY_MODE = "theme_mode"
    }
}
