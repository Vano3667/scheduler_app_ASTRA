package ru.dayplanner.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.dayplanner.app.ui.theme.AppStyle

/** Настройки приложения (SharedPreferences). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val styleState = MutableStateFlow(readStyle())
    val style: StateFlow<AppStyle> = styleState

    fun setStyle(style: AppStyle) {
        prefs.edit().putString(KEY_STYLE, style.name).apply()
        styleState.value = style
    }

    private fun readStyle(): AppStyle {
        val name = prefs.getString(KEY_STYLE, null) ?: return AppStyle.NORMAL
        return AppStyle.entries.firstOrNull { it.name == name } ?: AppStyle.NORMAL
    }

    private companion object {
        const val KEY_STYLE = "style"
    }
}
