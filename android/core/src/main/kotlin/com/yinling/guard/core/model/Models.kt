package com.yinling.guard.core.model

enum class KeywordCategory(val displayName: String, val value: String) {
    HEALTH_SCAM("养生骗局", "health_scam"),
    RUMOR("谣言", "rumor"),
    INCITEMENT("煽动", "incitement"),
    CLICKBAIT("标题党", "clickbait");

    companion object {
        fun fromValue(value: String): KeywordCategory? =
            entries.find { it.value == value }
    }
}

enum class WhitelistType(val value: String) {
    KEYWORD("keyword"),
    AUTHOR("author")
}

data class AppConfig(
    val onboardingCompleted: Boolean = false,
    val guardEnabled: Boolean = true,
    val toastEnabled: Boolean = true,
    val filterMode: String = "strict",
    val targetApp: String = "douyin",
    val firstGuardDate: String? = null,
    val familyPasswordHash: String? = null,
    val appVersion: String = "1.0.3"
)

data class KeywordEntry(
    val id: String,
    val word: String,
    val category: String,
    val source: String = "custom",
    val blockCount: Int = 0,
    val createdAt: String
)

data class KeywordStore(
    val version: Int = 1,
    val updatedAt: String,
    val keywords: List<KeywordEntry>
)

data class BlacklistAccount(
    val id: String,
    val authorName: String,
    val addedAt: String
)

data class BlacklistStore(
    val version: Int = 1,
    val updatedAt: String,
    val accounts: List<BlacklistAccount>
)

data class WhitelistEntry(
    val id: String,
    val type: String,
    val value: String,
    val addedAt: String
)

data class WhitelistStore(
    val version: Int = 1,
    val updatedAt: String,
    val entries: List<WhitelistEntry>
)

data class BlockLogEntry(
    val id: String,
    val timestamp: String,
    val title: String,
    val keyword: String,
    val category: String,
    val author: String
)

data class BlockLogStore(
    val version: Int = 1,
    val logs: List<BlockLogEntry>
)

data class BackupFile(
    val backupVersion: Int = 1,
    val exportedAt: String,
    val appVersion: String,
    val keywords: List<KeywordEntry>,
    val blacklist: List<BlacklistAccount>,
    val whitelist: List<WhitelistEntry>
)

data class ImportBackupResult(
    val success: Boolean,
    val message: String,
    val addedKeywords: Int = 0,
    val addedBlacklist: Int = 0,
    val addedWhitelist: Int = 0
)

data class VideoSnapshot(
    val title: String,
    val author: String,
    val packageName: String,
    val allText: String = ""
)

data class MatchResult(
    val matched: Boolean,
    val keyword: String = "",
    val category: String = "",
    val reason: MatchReason = MatchReason.NONE
)

enum class MatchReason {
    NONE,
    KEYWORD,
    BLACKLIST
}
