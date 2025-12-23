package com.github.kevinquillen.htaccess.http

import com.github.kevinquillen.htaccess.settings.HtaccessSettingsState
import org.junit.Assert.*
import org.junit.Test

class HtaccessSettingsTest {

    @Test
    fun `default values match settings state defaults`() {
        val settings = HtaccessSettings()

        assertEquals(HtaccessSettingsState.DEFAULT_TIMEOUT_MS.toLong(), settings.timeoutMs)
        assertEquals(HtaccessSettingsState.DEFAULT_MAX_RETRIES, settings.maxRetries)
        assertEquals(HtaccessSettingsState.DEFAULT_RETRY_DELAY_MS, settings.retryDelayMs)
    }

    @Test
    fun `custom values are preserved`() {
        val settings = HtaccessSettings(
            timeoutMs = 30_000L,
            maxRetries = 5,
            retryDelayMs = 2000
        )

        assertEquals(30_000L, settings.timeoutMs)
        assertEquals(5, settings.maxRetries)
        assertEquals(2000, settings.retryDelayMs)
    }

    @Test
    fun `settings equality works correctly`() {
        val settings1 = HtaccessSettings(timeoutMs = 5000L, maxRetries = 1, retryDelayMs = 500)
        val settings2 = HtaccessSettings(timeoutMs = 5000L, maxRetries = 1, retryDelayMs = 500)
        val settings3 = HtaccessSettings(timeoutMs = 10000L, maxRetries = 1, retryDelayMs = 500)

        assertEquals(settings1, settings2)
        assertNotEquals(settings1, settings3)
    }

    @Test
    fun `copy with modifications works`() {
        val original = HtaccessSettings(timeoutMs = 5000L, maxRetries = 1, retryDelayMs = 500)
        val modified = original.copy(timeoutMs = 10000L)

        assertEquals(10000L, modified.timeoutMs)
        assertEquals(1, modified.maxRetries)
        assertEquals(500, modified.retryDelayMs)
    }
}
