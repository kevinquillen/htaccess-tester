package com.github.kevinquillen.htaccess.settings

import org.junit.Assert.*
import org.junit.Test

class HtaccessSettingsStateTest {

    @Test
    fun `default values are set correctly`() {
        val state = HtaccessSettingsState()
        assertFalse(state.placeholder)
    }

    @Test
    fun `state equality works correctly`() {
        val state1 = HtaccessSettingsState()
        val state2 = HtaccessSettingsState()
        assertEquals(state1, state2)
    }
}
