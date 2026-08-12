package com.yinling.guard.core.engine

import com.yinling.guard.core.model.BlockLogEntry
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockLogRetentionTest {
    private val retention = BlockLogRetention()

    @Test
    fun `keeps only latest 1000 logs`() {
        val logs = (1..1001).map { index ->
            BlockLogEntry("log_$index", "2026-08-11T10:00:0${index % 10}", "title $index", "kw", "rumor", "author")
        }
        val pruned = retention.prune(logs)
        assertEquals(1000, pruned.size)
    }

    @Test
    fun `removes logs older than 30 days`() {
        val now = Instant.parse("2026-08-11T00:00:00Z")
        val logs = listOf(
            BlockLogEntry("1", "2026-07-01T10:00:00", "old", "k", "rumor", "a"),
            BlockLogEntry("2", "2026-08-10T10:00:00", "recent", "k", "rumor", "a")
        )
        val pruned = retention.prune(logs, now)
        assertEquals(1, pruned.size)
        assertEquals("2", pruned.first().id)
    }

    @Test
    fun `keeps logs within retention period`() {
        val now = Instant.parse("2026-08-11T12:00:00Z")
        val logs = listOf(
            BlockLogEntry("1", "2026-08-11T10:00:00", "t", "k", "rumor", "a"),
            BlockLogEntry("2", "2026-08-10T10:00:00", "t", "k", "rumor", "a")
        )
        assertEquals(2, retention.prune(logs, now).size)
    }

    @Test
    fun `handles empty list`() {
        assertTrue(retention.prune(emptyList()).isEmpty())
    }

    @Test
    fun `handles malformed timestamp gracefully`() {
        val logs = listOf(BlockLogEntry("1", "bad-timestamp", "t", "k", "rumor", "a"))
        assertEquals(1, retention.prune(logs, Instant.parse("2026-08-11T12:00:00Z")).size)
    }
}

class HomeStatsCalculatorTest {
    private val calculator = HomeStatsCalculator()

    @Test
    fun `calculates today count and guard days`() {
        val logs = listOf(
            BlockLogEntry("1", "2026-08-11T10:00:00", "t1", "k", "rumor", "a"),
            BlockLogEntry("2", "2026-08-11T12:00:00", "t2", "k", "rumor", "a"),
            BlockLogEntry("3", "2026-08-10T12:00:00", "t3", "k", "rumor", "a")
        )
        val stats = calculator.calculate(logs, "2026-08-09", LocalDate.parse("2026-08-11"))
        assertEquals(2, stats.todayBlockCount)
        assertEquals(3, stats.totalGuardDays)
        assertEquals(3, stats.recentBlocks.size)
    }

    @Test
    fun `total guard days minimum 1`() {
        val stats = calculator.calculate(emptyList(), "2026-08-11", LocalDate.parse("2026-08-11"))
        assertEquals(1, stats.totalGuardDays)
    }

    @Test
    fun `total guard days defaults to 1 when no first date`() {
        val stats = calculator.calculate(emptyList(), null, LocalDate.parse("2026-08-11"))
        assertEquals(1, stats.totalGuardDays)
    }

    @Test
    fun `recent blocks returns max 3`() {
        val logs = (1..10).map {
            BlockLogEntry("$it", "2026-08-11T10:00:00", "t", "k", "rumor", "a")
        }
        val stats = calculator.calculate(logs, "2026-08-01", LocalDate.parse("2026-08-11"))
        assertEquals(3, stats.recentBlocks.size)
    }

    @Test
    fun `recent blocks sorted by timestamp descending`() {
        val logs = listOf(
            BlockLogEntry("1", "2026-08-11T10:00:00", "t1", "k", "rumor", "a"),
            BlockLogEntry("2", "2026-08-11T14:00:00", "t2", "k", "rumor", "a"),
            BlockLogEntry("3", "2026-08-11T12:00:00", "t3", "k", "rumor", "a")
        )
        val stats = calculator.calculate(logs, "2026-08-01", LocalDate.parse("2026-08-11"))
        assertEquals("t2", stats.recentBlocks[0].title)
        assertEquals("t3", stats.recentBlocks[1].title)
        assertEquals("t1", stats.recentBlocks[2].title)
    }

    @Test
    fun `today count is 0 when no logs today`() {
        val logs = listOf(BlockLogEntry("1", "2026-08-10T10:00:00", "t", "k", "rumor", "a"))
        val stats = calculator.calculate(logs, "2026-08-01", LocalDate.parse("2026-08-11"))
        assertEquals(0, stats.todayBlockCount)
    }
}

class BlockLogFilterTest {
    private val filter = BlockLogFilter()
    private val now = LocalDate.parse("2026-08-11")

    private val logs = listOf(
        BlockLogEntry("1", "2026-08-11T10:00:00", "today1", "k", "rumor", "a"),
        BlockLogEntry("2", "2026-08-11T14:00:00", "today2", "k", "rumor", "a"),
        BlockLogEntry("3", "2026-08-10T10:00:00", "yesterday", "k", "rumor", "a"),
        BlockLogEntry("4", "2026-08-09T10:00:00", "older", "k", "rumor", "a")
    )

    @Test
    fun `filter today returns only today`() {
        val result = filter.filter(logs, BlockLogFilter.Range.TODAY, now)
        assertEquals(2, result.size)
        assertTrue(result.all { it.timestamp.startsWith("2026-08-11") })
    }

    @Test
    fun `filter yesterday returns only yesterday`() {
        val result = filter.filter(logs, BlockLogFilter.Range.YESTERDAY, now)
        assertEquals(1, result.size)
        assertEquals("yesterday", result[0].title)
    }

    @Test
    fun `filter all returns everything sorted descending`() {
        val result = filter.filter(logs, BlockLogFilter.Range.ALL, now)
        assertEquals(4, result.size)
        assertEquals("today2", result[0].title)
    }

    @Test
    fun `filter today returns empty when no logs today`() {
        val oldLogs = listOf(BlockLogEntry("1", "2026-08-01T10:00:00", "old", "k", "rumor", "a"))
        val result = filter.filter(oldLogs, BlockLogFilter.Range.TODAY, now)
        assertTrue(result.isEmpty())
    }
}
