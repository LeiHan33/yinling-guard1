package com.yinling.guard.core.engine

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoTextParserTest {

    private val parser = VideoTextParser()

    @Test
    fun `detects search page markers`() {
        assertTrue(parser.isSearchPageMarker("猜你想搜"))
        assertTrue(parser.isSearchPageMarker("历史搜索记录"))
    }

    @Test
    fun `filters douyin ui chrome before parsing`() {
        val result = parser.parse(listOf("关注", "点赞", "1234", "震惊！某国即将崩溃", "@作者昵称"))
        assertEquals("震惊！某国即将崩溃", result.title)
        assertTrue(result.allText.contains("震惊"))
        assertFalse(result.allText.contains("关注"))
    }

    @Test
    fun `parses title and author from texts`() {
        val result = parser.parse(listOf("震惊！某国即将崩溃", "张三"))
        assertEquals("震惊！某国即将崩溃", result.title)
        assertEquals("张三", result.author)
    }

    @Test
    fun `empty texts returns empty`() {
        val result = parser.parse(emptyList())
        assertEquals("", result.title)
        assertEquals("", result.author)
    }

    @Test
    fun `all blank texts returns empty`() {
        val result = parser.parse(listOf("", "  ", ""))
        assertEquals("", result.title)
        assertEquals("", result.author)
    }

    @Test
    fun `title detected by exclamation mark`() {
        val result = parser.parse(listOf("这是一个标题！"))
        assertEquals("这是一个标题！", result.title)
    }

    @Test
    fun `title detected by keyword marker`() {
        val result = parser.parse(listOf("震惊大新闻"))
        assertEquals("震惊大新闻", result.title)
    }

    @Test
    fun `title detected by length at least 8`() {
        val result = parser.parse(listOf("一二三四五六七八"))
        assertEquals("一二三四五六七八", result.title)
    }

    @Test
    fun `author is short text that does not look like title`() {
        val result = parser.parse(listOf("短标题！", "张三", "这是个很长的视频标题文本"))
        assertEquals("张三", result.author)
    }

    @Test
    fun `single long text becomes title`() {
        val result = parser.parse(listOf("这是一个足够长的视频标题"))
        assertEquals("这是一个足够长的视频标题", result.title)
    }

    @Test
    fun `single short text becomes author`() {
        val result = parser.parse(listOf("张三"))
        assertEquals("张三", result.author)
    }

    @Test
    fun `handles question mark marker`() {
        val result = parser.parse(listOf("这是真的吗？"))
        assertEquals("这是真的吗？", result.title)
    }
}
