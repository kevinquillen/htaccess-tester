package dev.kevinquillen.htaccess.settings

import org.junit.Assert.*
import org.junit.Test

class HtaccessSettingsStateTest {

    @Test
    fun `default values are set correctly`() {
        val state = HtaccessSettingsState()

        assertEquals(10_000, state.requestTimeoutMs)
        assertFalse(state.firstRunAcknowledged)
        assertEquals(2, state.maxRetryAttempts)
        assertEquals(1000, state.retryDelayMs)
    }

    @Test
    fun `state can be modified`() {
        val state = HtaccessSettingsState()

        state.requestTimeoutMs = 30_000
        state.firstRunAcknowledged = true
        state.maxRetryAttempts = 5
        state.retryDelayMs = 2000

        assertEquals(30_000, state.requestTimeoutMs)
        assertTrue(state.firstRunAcknowledged)
        assertEquals(5, state.maxRetryAttempts)
        assertEquals(2000, state.retryDelayMs)
    }

    @Test
    fun `state equality works correctly`() {
        val state1 = HtaccessSettingsState()
        val state2 = HtaccessSettingsState()

        assertEquals(state1, state2)

        state1.requestTimeoutMs = 5000
        assertNotEquals(state1, state2)
    }

    @Test
    fun `copy preserves values`() {
        val original = HtaccessSettingsState(
            requestTimeoutMs = 15_000,
            firstRunAcknowledged = true,
            maxRetryAttempts = 3,
            retryDelayMs = 500
        )

        val copy = original.copy()

        assertEquals(original, copy)
        assertEquals(15_000, copy.requestTimeoutMs)
        assertTrue(copy.firstRunAcknowledged)
    }

    @Test
    fun `default constants are accessible`() {
        assertEquals(10_000, HtaccessSettingsState.DEFAULT_TIMEOUT_MS)
        assertEquals(2, HtaccessSettingsState.DEFAULT_MAX_RETRIES)
        assertEquals(1000, HtaccessSettingsState.DEFAULT_RETRY_DELAY_MS)
    }
}
