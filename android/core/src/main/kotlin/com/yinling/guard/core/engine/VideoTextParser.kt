package com.yinling.guard.core.engine

data class ParsedVideoText(val title: String, val author: String)

class VideoTextParser {
    fun parse(texts: List<String>): ParsedVideoText {
        val cleaned = texts.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty()) return ParsedVideoText("", "")

        val author = cleaned.firstOrNull { it.length <= 20 && !looksLikeTitle(it) } ?: ""
        val title = cleaned.firstOrNull { looksLikeTitle(it) }
            ?: cleaned.maxByOrNull { it.length }
            ?: ""

        return ParsedVideoText(title = title, author = author)
    }

    private fun looksLikeTitle(text: String): Boolean {
        val markers = listOf("！", "!", "？", "?", "震惊", "消息", "曝光", "必看", "速看")
        return text.length >= 8 || markers.any { text.contains(it) }
    }
}
