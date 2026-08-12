package com.yinling.guard.core.engine

import com.yinling.guard.core.model.*
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GuardEngineTest {

    private fun kw(word: String, category: String = "rumor") = KeywordEntry(
        id = "kw_1", word = word, category = category, source = "builtin", createdAt = "2026-01-01"
    )

    private fun bl(authorName: String) = BlacklistAccount(
        id = "bl_1", authorName = authorName, addedAt = "2026-01-01"
    )

    private fun snap(title: String, author: String = "") =
        VideoSnapshot(title = title, author = author, packageName = ContentMatcher.DOUYIN_PACKAGE)

    private val fixedClock = { Instant.parse("2026-08-11T14:32:00Z") }
    private val fixedId = { "log_test_001" }

    @Test
    fun `blocks when keyword matches and guard enabled`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap("治百病"), true, listOf(kw("治百病")), emptyList(), emptyList())
        assertTrue(decision.shouldBlock)
        assertNotNull(decision.logEntry)
        assertEquals("治百病", decision.logEntry!!.keyword)
    }

    @Test
    fun `does not block when guard disabled`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap("治百病"), false, listOf(kw("治百病")), emptyList(), emptyList())
        assertFalse(decision.shouldBlock)
    }

    @Test
    fun `does not block when no match`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap("正常视频"), true, listOf(kw("治百病")), emptyList(), emptyList())
        assertFalse(decision.shouldBlock)
    }

    @Test
    fun `log entry contains correct data`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap("震惊标题", "某作者"), true, listOf(kw("震惊", "clickbait")), emptyList(), emptyList())
        val log = decision.logEntry!!
        assertEquals("log_test_001", log.id)
        assertEquals("震惊", log.keyword)
        assertEquals("clickbait", log.category)
        assertEquals("某作者", log.author)
    }

    @Test
    fun `log entry title truncated to 120 chars`() {
        val longTitle = "a".repeat(200)
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap(longTitle), true, listOf(kw("aa")), emptyList(), emptyList())
        assertTrue(decision.logEntry!!.title.length <= 120)
    }

    @Test
    fun `blacklist match creates log entry`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap("正常", "坏人"), true, emptyList(), listOf(bl("坏人")), emptyList())
        assertTrue(decision.shouldBlock)
        assertEquals("blacklist", decision.logEntry!!.category)
    }

    @Test
    fun `whitelist prevents blocking`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val wl = WhitelistEntry(id = "wl_1", type = "keyword", value = "官方", addedAt = "2026-01-01")
        val decision = engine.evaluate(
            snap("官方发布治百病"), true, listOf(kw("治百病")), emptyList(), listOf(wl)
        )
        assertFalse(decision.shouldBlock)
    }

    @Test
    fun `match result reason is correct for keyword`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap("震惊"), true, listOf(kw("震惊")), emptyList(), emptyList())
        assertEquals(MatchReason.KEYWORD, decision.matchResult.reason)
    }

    @Test
    fun `match result reason is correct for blacklist`() {
        val engine = GuardEngine(clock = fixedClock, idGenerator = fixedId)
        val decision = engine.evaluate(snap("正常", "坏人"), true, emptyList(), listOf(bl("坏人")), emptyList())
        assertEquals(MatchReason.BLACKLIST, decision.matchResult.reason)
    }
}
