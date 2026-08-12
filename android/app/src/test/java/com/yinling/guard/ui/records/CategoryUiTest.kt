package com.yinling.guard.ui.records

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryUiTest {
    @Test
    fun formatTime_extractsHourMinute() {
        assertEquals("14:32", CategoryUi.formatTime("2026-08-12T14:32:00"))
    }

    @Test
    fun displayName_mapsKnownCategory() {
        assertEquals("标题党", CategoryUi.displayName("clickbait"))
        assertEquals("黑名单", CategoryUi.displayName("blacklist"))
    }
}
