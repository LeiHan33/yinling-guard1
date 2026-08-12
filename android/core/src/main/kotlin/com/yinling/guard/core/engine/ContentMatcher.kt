package com.yinling.guard.core.engine

import com.yinling.guard.core.model.BlacklistAccount
import com.yinling.guard.core.model.KeywordEntry
import com.yinling.guard.core.model.MatchReason
import com.yinling.guard.core.model.MatchResult
import com.yinling.guard.core.model.VideoSnapshot
import com.yinling.guard.core.model.WhitelistEntry

class ContentMatcher {
    fun match(
        snapshot: VideoSnapshot,
        keywords: List<KeywordEntry>,
        blacklist: List<BlacklistAccount>,
        whitelist: List<WhitelistEntry>,
        filterMode: String = "strict",
        targetPackage: String = DOUYIN_PACKAGE
    ): MatchResult {
        if (snapshot.packageName != targetPackage) {
            return MatchResult(matched = false)
        }

        if (!snapshot.inFeedContext) {
            return MatchResult(matched = false)
        }

        val title = snapshot.title.trim()
        val author = snapshot.author.trim()
        if (title.isEmpty() && author.isEmpty()) {
            return MatchResult(matched = false)
        }

        if (isWhitelisted(title, author, whitelist)) {
            return MatchResult(matched = false)
        }

        blacklist.firstOrNull { account ->
            author.isNotEmpty() && author.contains(account.authorName, ignoreCase = true)
        }?.let {
            return MatchResult(
                matched = true,
                keyword = it.authorName,
                category = "blacklist",
                reason = MatchReason.BLACKLIST
            )
        }

        keywords.forEach { entry ->
            if (matchesKeyword(entry, snapshot, filterMode)) {
                return MatchResult(
                    matched = true,
                    keyword = entry.word,
                    category = entry.category,
                    reason = MatchReason.KEYWORD
                )
            }
        }

        return MatchResult(matched = false)
    }

    private fun matchesKeyword(
        entry: KeywordEntry,
        snapshot: VideoSnapshot,
        filterMode: String
    ): Boolean {
        val word = entry.word.trim()
        if (word.length < MIN_KEYWORD_LENGTH) return false

        val title = snapshot.title.trim()
        val author = snapshot.author.trim()

        if (title.isNotEmpty() && containsKeyword(title, word)) {
            return true
        }

        if (author.isNotEmpty() && word.length >= AUTHOR_KEYWORD_MIN_LENGTH && containsKeyword(author, word)) {
            return true
        }

        if (filterMode != "strict"
            && snapshot.hasCaptionEvidence
            && title.isEmpty()
            && author.isEmpty()
        ) {
            val fallbackText = snapshot.allText.trim()
            if (fallbackText.isNotEmpty() && containsKeyword(fallbackText, word)) {
                return true
            }
        }

        return false
    }

    private fun containsKeyword(text: String, keyword: String): Boolean {
        return text.contains(keyword, ignoreCase = true)
    }

    private fun isWhitelisted(
        title: String,
        author: String,
        whitelist: List<WhitelistEntry>
    ): Boolean {
        return whitelist.any { entry ->
            when (entry.type) {
                "keyword" -> title.contains(entry.value, ignoreCase = true)
                "author" -> author.contains(entry.value, ignoreCase = true)
                else -> false
            }
        }
    }

    companion object {
        const val DOUYIN_PACKAGE = "com.ss.android.ugc.aweme"
        private const val MIN_KEYWORD_LENGTH = 2
        private const val AUTHOR_KEYWORD_MIN_LENGTH = 3
    }
}
