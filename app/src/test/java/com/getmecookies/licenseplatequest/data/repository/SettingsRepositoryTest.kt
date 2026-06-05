package com.getmecookies.licenseplatequest.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Tests for the SharedPreferences-backed settings store: sensible defaults, and that each setting
 * persists (a fresh instance reads the saved value). Run under Robolectric for a real Context.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = SettingsRepository(context)
    }

    @Test
    fun defaults() {
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.value)
        assertTrue(repo.hapticsEnabled.value)
        assertTrue(repo.tripRemindersEnabled.value)
        assertNull(repo.home.value)
    }

    @Test
    fun themeMode_persistsAcrossInstances() {
        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.themeMode.value)
        assertEquals(ThemeMode.DARK, SettingsRepository(context).themeMode.value)
    }

    @Test
    fun haptics_persistsAcrossInstances() {
        repo.setHapticsEnabled(false)
        assertFalse(repo.hapticsEnabled.value)
        assertFalse(SettingsRepository(context).hapticsEnabled.value)
    }

    @Test
    fun tripReminders_persistsAcrossInstances() {
        repo.setTripRemindersEnabled(false)
        assertFalse(repo.tripRemindersEnabled.value)
        assertFalse(SettingsRepository(context).tripRemindersEnabled.value)
    }

    @Test
    fun home_setPersistsAndClears() {
        val regionId = UUID.randomUUID()
        repo.setHome(regionId, "Austin")

        assertEquals(regionId, repo.home.value?.regionId)
        assertEquals("Austin", repo.home.value?.city)
        // Persisted to a fresh instance.
        assertEquals(regionId, SettingsRepository(context).home.value?.regionId)

        repo.clearHome()
        assertNull(repo.home.value)
    }
}
