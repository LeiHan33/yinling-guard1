package com.yinling.guard.core.engine

import com.yinling.guard.core.model.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentMatcherTest {

    private val matcher = ContentMatcher()

    private fun kw(word: String, category: String = "rumor") = KeywordEntry(
        id = "kw_1", word = word, category = category, source = "builtin", createdAt = "2026-01-01"
    )

    private fun bl(authorName: String) = BlacklistAccount(
        id = "bl_1", authorName = authorName, addedAt = "2026-01-01"
    )

    private fun wl(type: String, value: String) = WhitelistEntry(
        id = "wl_1", type = type, value = value, addedAt = "2026-01-01"
    )

    private fun snap(title: String, author: String = "", pkg: String = ContentMatcher.DOUYIN_PACKAGE) =
        VideoSnapshot(title = title, author = author, packageName = pkg)

    // ========== 关键词匹配 ==========

    @Test
    fun `keyword match when title contains keyword`() {
        val result = matcher.match(snap("这个能治百病"), listOf(kw("治百病")), emptyList(), emptyList())
        assertTrue(result.matched)
        assertEquals("治百病", result.keyword)
        assertEquals(MatchReason.KEYWORD, result.reason)
    }

    @Test
    fun `no match when title has no keywords`() {
        val result = matcher.match(snap("今天天气好"), listOf(kw("治百病")), emptyList(), emptyList())
        assertFalse(result.matched)
    }

    @Test
    fun `match is case insensitive`() {
        val result = matcher.match(snap("this has ABC"), listOf(kw("abc")), emptyList(), emptyList())
        assertTrue(result.matched)
    }

    @Test
    fun `match returns first matching keyword`() {
        val result = matcher.match(
            snap("包含第一和第二"), listOf(kw("第一"), kw("第二")), emptyList(), emptyList()
        )
        assertEquals("第一", result.keyword)
    }

    @Test
    fun `empty title and author returns no match`() {
        val result = matcher.match(snap(""), listOf(kw("测试")), emptyList(), emptyList())
        assertFalse(result.matched)
    }

    @Test
    fun `keyword match returns correct category`() {
        val result = matcher.match(
            snap("震惊！"), listOf(kw("震惊", "clickbait")), emptyList(), emptyList()
        )
        assertEquals("clickbait", result.category)
    }

    // ========== 黑名单匹配 ==========

    @Test
    fun `blacklist match when author matches`() {
        val result = matcher.match(snap("正常标题", "坏人"), emptyList(), listOf(bl("坏人")), emptyList())
        assertTrue(result.matched)
        assertEquals(MatchReason.BLACKLIST, result.reason)
    }

    @Test
    fun `blacklist ignores empty author`() {
        val result = matcher.match(snap("正常标题", ""), emptyList(), listOf(bl("坏人")), emptyList())
        assertFalse(result.matched)
    }

    @Test
    fun `blacklist case insensitive`() {
        val result = matcher.match(snap("正常标题", "abc"), emptyList(), listOf(bl("ABC")), emptyList())
        assertTrue(result.matched)
    }

    @Test
    fun `blacklist partial match`() {
        val result = matcher.match(snap("正常标题", "老王养生堂"), emptyList(), listOf(bl("养生堂")), emptyList())
        assertTrue(result.matched)
    }

    @Test
    fun `blacklist returns category as blacklist`() {
        val result = matcher.match(snap("正常标题", "坏人"), emptyList(), listOf(bl("坏人")), emptyList())
        assertEquals("blacklist", result.category)
    }

    // ========== 白名单放行 ==========

    @Test
    fun `whitelist overrides keyword match`() {
        val result = matcher.match(
            snap("官方发布治百病政策"), listOf(kw("治百病")), emptyList(), listOf(wl("keyword", "官方发布"))
        )
        assertFalse(result.matched)
    }

    @Test
    fun `whitelist overrides blacklist match`() {
        val result = matcher.match(
            snap("正常标题", "坏人-官方认证"), emptyList(), listOf(bl("坏人")), listOf(wl("author", "官方认证"))
        )
        assertFalse(result.matched)
    }

    @Test
    fun `whitelist does not affect non-matching content`() {
        val result = matcher.match(
            snap("震惊！"), listOf(kw("震惊")), emptyList(), listOf(wl("keyword", "官方"))
        )
        assertTrue(result.matched)
    }

    @Test
    fun `empty whitelist does not block`() {
        val result = matcher.match(snap("测试"), listOf(kw("测试")), emptyList(), emptyList())
        assertTrue(result.matched)
    }

    @Test
    fun `keyword whitelist only matches title`() {
        val result = matcher.match(
            snap("普通标题", "官方发布"), listOf(kw("普通")), emptyList(), listOf(wl("keyword", "官方发布"))
        )
        assertTrue(result.matched) // keyword whitelist checks title, not author
    }

    @Test
    fun `author whitelist only matches author`() {
        val result = matcher.match(
            snap("官方发布消息"), listOf(kw("官方")), emptyList(), listOf(wl("author", "人民日报"))
        )
        assertTrue(result.matched) // author whitelist doesn't match title
    }

    // ========== 包名过滤 ==========

    @Test
    fun `non-douyin package returns no match`() {
        val result = matcher.match(
            snap("治百病", pkg = "com.other.app"), listOf(kw("治百病")), emptyList(), emptyList()
        )
        assertFalse(result.matched)
    }

    @Test
    fun `douyin package matches`() {
        val result = matcher.match(snap("治百病"), listOf(kw("治百病")), emptyList(), emptyList())
        assertTrue(result.matched)
    }

    // ========== 综合场景 ==========

    @Test
    fun `blacklist takes priority over keyword`() {
        val result = matcher.match(
            snap("震惊！", "某作者"), listOf(kw("震惊")), listOf(bl("某作者")), emptyList()
        )
        assertEquals(MatchReason.BLACKLIST, result.reason)
    }

    @Test
    fun `empty everything returns no match`() {
        val result = matcher.match(snap("", ""), emptyList(), emptyList(), emptyList())
        assertFalse(result.matched)
    }

    @Test
    fun `title with only spaces returns no match`() {
        val result = matcher.match(snap("   ", "  "), listOf(kw("测试")), emptyList(), emptyList())
        assertFalse(result.matched)
    }

    @Test
    fun `multiple keywords match first one`() {
        val kws = listOf(kw("第一个"), kw("第二个"), kw("第三个"))
        val result = matcher.match(snap("包含第三个和第一个"), kws, emptyList(), emptyList())
        assertEquals("第一个", result.keyword)
    }

    @Test
    fun `blacklist match returns author name as keyword`() {
        val result = matcher.match(snap("正常", "坏人"), emptyList(), listOf(bl("坏人")), emptyList())
        assertEquals("坏人", result.keyword)
    }
}
