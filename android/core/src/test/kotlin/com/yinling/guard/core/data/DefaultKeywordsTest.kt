package com.yinling.guard.core.data

import com.yinling.guard.core.model.KeywordCategory
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultKeywordsTest {
    @Test
    fun `builtin keyword categories cover four groups`() {
        val categories = KeywordCategory.entries.map { it.value }.toSet()
        assertTrue(categories.contains("health_scam"))
        assertTrue(categories.contains("rumor"))
        assertTrue(categories.contains("incitement"))
        assertTrue(categories.contains("clickbait"))
    }
}
