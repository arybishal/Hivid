package com.hivid.app

import com.hivid.app.data.preferences.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {

    @Test
    fun testDefaultSettings() {
        val settings = UserSettings()
        assertEquals("DARK", settings.themeMode)
        assertEquals("HDR10", settings.defaultProfile)
        assertEquals(70f, settings.defaultExpansion, 0.001f)
        assertEquals(60, settings.fpsCap)
        assertEquals(0, settings.manualBitrateOverride)
        assertTrue(settings.keepScreenOn)
        assertTrue(settings.notificationsEnabled)
    }

    @Test
    fun testLightModeSettings() {
        val settings = UserSettings(themeMode = "LIGHT")
        assertEquals("LIGHT", settings.themeMode)
        assertFalse(settings.themeMode != "LIGHT")
    }

    @Test
    fun testDarkModeSettings() {
        val settings = UserSettings(themeMode = "DARK")
        assertEquals("DARK", settings.themeMode)
        assertTrue(settings.themeMode != "LIGHT")
    }
}
