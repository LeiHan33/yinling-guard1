package com.yinling.guard.util

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.yinling.guard.core.engine.DouyinScreenContext
import com.yinling.guard.core.engine.ParsedVideoText
import com.yinling.guard.core.engine.VideoTextParser

object DouyinNodeParser {
    private val parser = VideoTextParser()

    private val CAPTION_VIEW_ID_HINTS = listOf(
        "desc", "feed_desc", "video_desc", "tv_desc", "content_desc", "video_title"
    )
    private val AUTHOR_VIEW_ID_HINTS = listOf(
        "user_name", "author", "nickname", "account", "publisher"
    )
    private val AUTHOR_TITLE_HINTS = listOf(
        "/title", ":id/title", "user_title", "author_name"
    )
    private val SEARCH_VIEW_ID_HINTS = listOf(
        "search", "et_search", "search_input", "search_bar", "search_edit"
    )

    fun parse(root: AccessibilityNodeInfo?): ParsedVideoText {
        if (root == null) return ParsedVideoText("", "")

        val screenHeight = Rect().also { root.getBoundsInScreen(it) }.height().coerceAtLeast(1)
        val nodes = mutableListOf<CollectedNode>()
        collectNodes(root, nodes, screenHeight)

        val screenContext = detectScreenContext(nodes, root)
        if (screenContext != DouyinScreenContext.VIDEO_FEED) {
            return ParsedVideoText(
                title = "",
                author = "",
                allText = "",
                screenContext = screenContext,
                hasCaptionEvidence = false
            )
        }

        val contentNodes = nodes.filter { !it.isEditable && !it.isInputField }
        val captionFromId = findTextByViewIdHints(contentNodes, CAPTION_VIEW_ID_HINTS)
        val authorFromId = findAuthorText(contentNodes)
        val captionCandidates = contentNodes
            .filter { it.inCaptionRegion }
            .map { it.text }
            .filter { it.isNotBlank() && !parser.isUiNoise(it) }

        val texts = buildList {
            captionFromId?.let { add(it) }
            authorFromId?.let { add(it) }
            addAll(captionCandidates)
            addAll(contentNodes.map { it.text }.filter { it.isNotBlank() })
        }

        val parsed = parser.parse(texts)
        val title = captionFromId?.takeIf { it.isNotBlank() }
            ?: captionCandidates.maxByOrNull { it.length }
            ?: parsed.title
        val author = authorFromId?.takeIf { it.isNotBlank() } ?: parsed.author
        val hasCaptionEvidence = !captionFromId.isNullOrBlank()
            || captionCandidates.isNotEmpty()
            || title.length >= 4

        return parsed.copy(
            title = title,
            author = author,
            screenContext = DouyinScreenContext.VIDEO_FEED,
            hasCaptionEvidence = hasCaptionEvidence
        )
    }

    private fun detectScreenContext(
        nodes: List<CollectedNode>,
        root: AccessibilityNodeInfo
    ): DouyinScreenContext {
        val allTexts = nodes.map { it.text }
        val viewIds = nodes.mapNotNull { it.viewId?.lowercase() }
        val rootClass = root.className?.toString().orEmpty()

        if (rootClass.contains("Search", ignoreCase = true)) {
            return DouyinScreenContext.SEARCH
        }

        if (viewIds.any { viewId ->
                SEARCH_VIEW_ID_HINTS.any { hint ->
                    viewId.contains(hint) && !viewId.contains("research")
                }
            }
        ) {
            return DouyinScreenContext.SEARCH
        }

        if (allTexts.any { parser.isSearchPageMarker(it) }) {
            return DouyinScreenContext.SEARCH
        }

        if (nodes.any { it.isInputField || it.isEditable }) {
            return DouyinScreenContext.SEARCH
        }

        val hasSearchTabs = SEARCH_RESULT_TABS.all { tab ->
            allTexts.any { it.equals(tab, ignoreCase = true) }
        }
        if (hasSearchTabs) {
            return DouyinScreenContext.SEARCH
        }

        val hasFeedChrome = FEED_CHROME.any { marker -> allTexts.any { it.contains(marker) } }
        val hasCaptionNode = nodes.any { node ->
            CAPTION_VIEW_ID_HINTS.any { hint -> node.viewId?.lowercase()?.contains(hint) == true }
        }
        val hasAuthorNode = nodes.any { node ->
            AUTHOR_VIEW_ID_HINTS.any { hint -> node.viewId?.lowercase()?.contains(hint) == true }
                || AUTHOR_TITLE_HINTS.any { hint -> node.viewId?.lowercase()?.contains(hint) == true }
        }
        val hasCaptionRegionText = nodes.any { it.inCaptionRegion && it.text.length >= 4 }

        if (hasCaptionNode || hasCaptionRegionText || (hasFeedChrome && (hasAuthorNode || hasAuthorLikeText(allTexts)))) {
            return DouyinScreenContext.VIDEO_FEED
        }

        return DouyinScreenContext.OTHER
    }

    private fun hasAuthorLikeText(texts: List<String>): Boolean {
        return texts.any { it.startsWith("@") || (it.length in 2..20 && !parser.isUiNoise(it)) }
    }

    private fun findAuthorText(nodes: List<CollectedNode>): String? {
        findTextByViewIdHints(nodes, AUTHOR_VIEW_ID_HINTS)?.let { return it }
        return nodes
            .filter { node ->
                val viewId = node.viewId?.lowercase().orEmpty()
                AUTHOR_TITLE_HINTS.any { hint -> viewId.contains(hint) }
            }
            .map { it.text.trim() }
            .firstOrNull { it.isNotEmpty() && !parser.isUiNoise(it) }
    }

    private fun findTextByViewIdHints(
        nodes: List<CollectedNode>,
        hints: List<String>
    ): String? {
        return nodes
            .filter { node ->
                val viewId = node.viewId?.lowercase().orEmpty()
                hints.any { hint -> viewId.contains(hint) }
            }
            .map { it.text.trim() }
            .firstOrNull { it.isNotEmpty() && !parser.isUiNoise(it) }
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        out: MutableList<CollectedNode>,
        screenHeight: Int
    ) {
        val className = node.className?.toString().orEmpty()
        val isEditable = node.isEditable
        val isInputField = className.contains("EditText", ignoreCase = true)
            || className.contains("AutoCompleteTextView", ignoreCase = true)
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val inCaptionRegion = bounds.top >= screenHeight * 0.45f

        if (!isInputField) {
            node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
                out.add(
                    CollectedNode(
                        text = text,
                        viewId = node.viewIdResourceName,
                        className = className,
                        isEditable = isEditable,
                        isInputField = isInputField,
                        inCaptionRegion = inCaptionRegion
                    )
                )
            }
            node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
                if (out.none { it.text == text }) {
                    out.add(
                        CollectedNode(
                            text = text,
                            viewId = node.viewIdResourceName,
                            className = className,
                            isEditable = isEditable,
                            isInputField = isInputField,
                            inCaptionRegion = inCaptionRegion
                        )
                    )
                }
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, out, screenHeight) }
        }
    }

    private data class CollectedNode(
        val text: String,
        val viewId: String?,
        val className: String,
        val isEditable: Boolean,
        val isInputField: Boolean,
        val inCaptionRegion: Boolean
    )

    private val FEED_CHROME = listOf("点赞", "评论", "分享", "收藏", "拍同款")
    private val SEARCH_RESULT_TABS = listOf("综合", "用户", "视频")
}
