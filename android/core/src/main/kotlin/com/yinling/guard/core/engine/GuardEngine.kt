package com.yinling.guard.core.engine

import com.yinling.guard.core.model.BlockLogEntry
import com.yinling.guard.core.model.MatchResult
import com.yinling.guard.core.model.VideoSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class GuardEngine(
    private val contentMatcher: ContentMatcher = ContentMatcher(),
    private val clock: () -> Instant = { Instant.now() },
    private val idGenerator: () -> String = { "log_${UUID.randomUUID()}" }
) {
    data class GuardDecision(
        val shouldBlock: Boolean,
        val logEntry: BlockLogEntry?,
        val matchResult: MatchResult
    )

    fun evaluate(
        snapshot: VideoSnapshot,
        guardEnabled: Boolean,
        keywords: List<com.yinling.guard.core.model.KeywordEntry>,
        blacklist: List<com.yinling.guard.core.model.BlacklistAccount>,
        whitelist: List<com.yinling.guard.core.model.WhitelistEntry>,
        filterMode: String = "strict"
    ): GuardDecision {
        if (!guardEnabled) {
            return GuardDecision(false, null, MatchResult(matched = false))
        }

        val matchResult = contentMatcher.match(
            snapshot,
            keywords,
            blacklist,
            whitelist,
            filterMode
        )
        if (!matchResult.matched) {
            return GuardDecision(false, null, matchResult)
        }

        val now = clock()
        val timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            now.atZone(ZoneId.systemDefault()).toLocalDateTime()
        )

        val logEntry = BlockLogEntry(
            id = idGenerator(),
            timestamp = timestamp,
            title = snapshot.title.take(120),
            keyword = matchResult.keyword,
            category = matchResult.category,
            author = snapshot.author
        )

        return GuardDecision(true, logEntry, matchResult)
    }
}

class BlockLogRetention {
    companion object {
        const val MAX_LOG_COUNT = 1000
        const val RETENTION_DAYS = 30L
    }

    fun prune(logs: List<BlockLogEntry>, now: Instant = Instant.now()): List<BlockLogEntry> {
        val zone = ZoneId.systemDefault()
        val cutoffDate = now.atZone(zone).toLocalDate().minusDays(RETENTION_DAYS)

        val filtered = logs.filter { entry ->
            runCatching {
                LocalDate.parse(entry.timestamp.substring(0, 10)) >= cutoffDate
            }.getOrDefault(true)
        }

        return if (filtered.size <= MAX_LOG_COUNT) {
            filtered
        } else {
            filtered.takeLast(MAX_LOG_COUNT)
        }
    }
}

class HomeStatsCalculator {
    data class HomeStats(
        val todayBlockCount: Int,
        val totalGuardDays: Int,
        val recentBlocks: List<BlockLogEntry>
    )

    fun calculate(
        logs: List<BlockLogEntry>,
        firstGuardDate: String?,
        now: LocalDate = LocalDate.now()
    ): HomeStats {
        val today = now.toString()
        val todayCount = logs.count { it.timestamp.startsWith(today) }
        val totalDays = firstGuardDate?.let { first ->
            runCatching {
                val start = LocalDate.parse(first)
                (now.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)
            }.getOrDefault(1)
        } ?: 1

        val recent = logs.sortedByDescending { it.timestamp }.take(3)
        return HomeStats(todayCount, totalDays, recent)
    }
}

class BlockLogFilter {
    enum class Range { TODAY, YESTERDAY, ALL }

    fun filter(logs: List<BlockLogEntry>, range: Range, now: LocalDate = LocalDate.now()): List<BlockLogEntry> {
        return when (range) {
            Range.ALL -> logs.sortedByDescending { it.timestamp }
            Range.TODAY -> logs.filter { it.timestamp.startsWith(now.toString()) }
                .sortedByDescending { it.timestamp }
            Range.YESTERDAY -> {
                val yesterday = now.minusDays(1).toString()
                logs.filter { it.timestamp.startsWith(yesterday) }.sortedByDescending { it.timestamp }
            }
        }
    }
}
