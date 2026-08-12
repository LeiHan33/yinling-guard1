package com.yinling.guard.core.engine

data class ParsedVideoText(
    val title: String,
    val author: String,
    val allText: String = ""
)

class VideoTextParser {
    fun parse(texts: List<String>): ParsedVideoText {
        val cleaned = texts
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { isUiNoise(it) }
            .distinct()
        if (cleaned.isEmpty()) return ParsedVideoText("", "")

        val author = cleaned.firstOrNull { looksLikeAuthor(it) }
            ?: cleaned.firstOrNull { it.length <= 20 && !looksLikeTitle(it) }
            ?: ""
        val title = cleaned.firstOrNull { looksLikeTitle(it) && it != author }
            ?: cleaned.filter { it != author }.maxByOrNull { it.length }
            ?: cleaned.maxByOrNull { it.length }
            ?: ""

        val allText = cleaned.joinToString(" ")
        return ParsedVideoText(title = title, author = author, allText = allText)
    }

    fun isUiNoise(text: String): Boolean {
        if (text.length <= 1) return true
        if (text in UI_NOISE) return true
        if (text.matches(Regex("^\\d+[\\s]*[万wW+]?$"))) return true
        if (text.matches(Regex("^(点赞|评论|分享|收藏|转发)\\s*\\d+[\\s\\S]*$"))) return true
        return false
    }

    private fun looksLikeAuthor(text: String): Boolean {
        return text.startsWith("@") || (text.length <= 16 && !looksLikeTitle(text))
    }

    private fun looksLikeTitle(text: String): Boolean {
        val markers = listOf(
            "！", "!", "？", "?", "震惊", "消息", "曝光", "必看", "速看",
            "谣言", "骗局", "养生", "秘方", "崩溃", "真相", "删除"
        )
        return text.length >= 6 || markers.any { text.contains(it) }
    }

    companion object {
        private val UI_NOISE = setOf(
            "关注", "已关注", "点赞", "评论", "分享", "收藏", "拍同款", "转发",
            "首页", "朋友", "消息", "我", "搜索", "直播", "推荐", "商城",
            "关注中", "送礼物", "全屏观看", "展开", "收起", "合集",
            "Follow", "Like", "Comment", "Share", "Live"
        )
    }
}
