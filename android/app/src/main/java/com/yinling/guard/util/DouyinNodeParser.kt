package com.yinling.guard.util

import android.view.accessibility.AccessibilityNodeInfo
import com.yinling.guard.core.engine.VideoTextParser

object DouyinNodeParser {
    private val parser = VideoTextParser()

    fun parse(root: AccessibilityNodeInfo?): com.yinling.guard.core.engine.ParsedVideoText {
        if (root == null) return com.yinling.guard.core.engine.ParsedVideoText("", "")

        val texts = mutableListOf<String>()
        collectTexts(root, texts)
        return parser.parse(texts)
    }

    private fun collectTexts(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.toString()?.let { out.add(it) }
        node.contentDescription?.toString()?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectTexts(it, out) }
        }
    }
}
