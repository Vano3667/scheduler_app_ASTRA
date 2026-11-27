package ru.dayplanner.app.ui.theme

import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {
    @Test fun manualChoiceOverridesBothDeviceModes() {
        for (systemDark in listOf(true, false)) {
            assertTrue(ThemeMode.DARK.isDark(systemDark)); assertFalse(ThemeMode.LIGHT.isDark(systemDark))
        }
    }
    @Test fun freshOrUpgradedInstallFollowsSystem() {
        for (stored in listOf(null, "", "unknown", "SYSTEM")) for (systemDark in listOf(true, false)) {
            assertEquals(systemDark, ThemeMode.fromPreference(stored).isDark(systemDark))
        }
    }
    @Test fun persistedChoicesRoundTrip() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromPreference(it.name)) }
    }
}
