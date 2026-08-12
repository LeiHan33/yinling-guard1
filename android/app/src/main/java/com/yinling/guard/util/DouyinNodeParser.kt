package com.yinling.guard.util

import android.view.accessibility.AccessibilityNodeInfo
import com.yinling.guard.core.engine.ParsedVideoText
import com.yinling.guard.core.engine.VideoTextParser

object DouyinNodeParser {
    private val parser = VideoTextParser()

    private val CAPTION_VIEW_ID_HINTS = listOf(
        "desc", "feed_desc", "video_desc", "tv_desc", "content_desc", "video_title"
    )
    private val AUTHOR_VIEW_ID_HINTS = listOf(
        "title", "user_name", "author", "nickname", "name", "account"
    )

    fun parse(root: AccessibilityNodeInfo?): ParsedVideoText {
        if (root == null) return ParsedVideoText("", "")

        val nodes = mutableListOf<CollectedNode>()
        collectNodes(root, nodes)

        val contentNodes = nodes.filter { !it.isEditable && !it.isInputField }
        val captionFromId = findTextByViewIdHints(contentNodes, CAPTION_VIEW_ID_HINTS)
        val authorFromId = findTextByViewIdHints(contentNodes, AUTHOR_VIEW_ID_HINTS)

        if (!captionFromId.isNullOrBlank() || !authorFromId.isNullOrBlank()) {
            val texts = buildList {
                captionFromId?.let { add(it) }
                authorFromId?.let { add(it) }
                addAll(contentNodes.map { it.text }.filter { it.isNotBlank() })
            }
            val parsed = parser.parse(texts)
            return parsed.copy(
                title = captionFromId?.takeIf { it.isNotBlank() } ?: parsed.title,
                author = authorFromId?.takeIf { it.isNotBlank() } ?: parsed.author
            )
        }

        return parser.parse(contentNodes.map { it.text })
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

    private fun collectNodes(node: AccessibilityNodeInfo, out: MutableList<CollectedNode>) {
        val className = node.className?.toString().orEmpty()
        val isEditable = node.isEditable
        val isInputField = className.contains("EditText", ignoreCase = true)
                || className.contains("AutoCompleteTextView", ignoreCase = true)

        if (!isInputField) {
            node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
                out.add(
                    CollectedNode(
                        text = text,
                        viewId = node.viewIdResourceName,
                        className = className,
                        isEditable = isEditable,
                        isInputField = isInputField
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
                            isInputField = isInputField
                        )
                    )
                }
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, out) }
        }
    }

    private data class CollectedNode(
        val text: String,
        val viewId: String?,
        val className: String,
        val isEditable: Boolean,
        val isInputField: Boolean
    )
}
