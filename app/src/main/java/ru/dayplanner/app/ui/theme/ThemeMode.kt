package ru.dayplanner.app.ui.theme

/** Светлая/тёмная тема не связана с выбором стиля и эффектов. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromPreference(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
