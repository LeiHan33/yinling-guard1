package com.yinling.guard.core.storage

import com.yinling.guard.core.model.*
import com.google.gson.GsonBuilder
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuardRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repository: GuardRepository

    @Before
    fun setup() {
        tempDir = createTempDir("repo_test")
        val fileReader = object : FileReader {
            private fun file(path: String) = File(tempDir, path)
            override fun exists(path: String) = file(path).exists()
            override fun readText(path: String) = file(path).readText()
            override fun writeText(path: String, content: String) {
                file(path).parentFile?.mkdirs()
                file(path).writeText(content)
            }
            override fun readAsset(assetName: String): ByteArrayInputStream? = null
        }
        repository = GuardRepository(fileReader, nowProvider = { Instant.parse("2026-08-11T12:00:00Z") })
    }

    @Test
    fun `loadConfig returns defaults when no file`() {
        val config = repository.loadConfig()
        assertFalse(config.onboardingCompleted)
        assertTrue(config.guardEnabled)
        assertTrue(config.toastEnabled)
    }

    @Test
    fun `saveConfig then loadConfig persists data`() {
        val config = AppConfig(onboardingCompleted = true, guardEnabled = false)
        repository.saveConfig(config)
        val loaded = repository.loadConfig()
        assertTrue(loaded.onboardingCompleted)
        assertFalse(loaded.guardEnabled)
    }

    @Test
    fun `loadKeywords returns empty when no file and no asset`() {
        val store = repository.loadKeywords()
        assertTrue(store.keywords.isEmpty())
    }

    @Test
    fun `addKeyword persists new keyword`() {
        val kw = repository.addKeyword("测试词", "rumor")
        assertTrue(repository.loadKeywords().keywords.any { it.id == kw.id })
    }

    @Test
    fun `addKeyword returns entry with correct data`() {
        val kw = repository.addKeyword("测试", "clickbait")
        assertEquals("测试", kw.word)
        assertEquals("clickbait", kw.category)
        assertEquals("custom", kw.source)
        assertEquals(0, kw.blockCount)
    }

    @Test
    fun `addKeyword does not deduplicate by word`() {
        repository.addKeyword("重复", "rumor")
        repository.addKeyword("重复", "clickbait")
        val count = repository.loadKeywords().keywords.count { it.word == "重复" }
        assertEquals(2, count) // repository does not deduplicate, FamilyManager does
    }

    @Test
    fun `removeKeyword removes by id`() {
        val kw = repository.addKeyword("要删", "rumor")
        repository.removeKeyword(kw.id)
        assertFalse(repository.loadKeywords().keywords.any { it.id == kw.id })
    }

    @Test
    fun `loadBlacklist returns empty when no file`() {
        val store = repository.loadBlacklist()
        assertTrue(store.accounts.isEmpty())
    }

    @Test
    fun `saveBlacklist then loadBlacklist persists`() {
        val store = BlacklistStore(updatedAt = "2026-08-11", accounts = listOf(
            BlacklistAccount("bl_1", "坏人", "2026-08-11")
        ))
        repository.saveBlacklist(store)
        val loaded = repository.loadBlacklist()
        assertEquals(1, loaded.accounts.size)
        assertEquals("坏人", loaded.accounts[0].authorName)
    }

    @Test
    fun `loadWhitelist returns empty when no file`() {
        val store = repository.loadWhitelist()
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun `loadBlockLogs returns empty when no file`() {
        val store = repository.loadBlockLogs()
        assertTrue(store.logs.isEmpty())
    }

    @Test
    fun `appendBlockLog persists entry`() {
        val entry = BlockLogEntry("log_1", "2026-08-11T10:00:00", "标题", "关键词", "rumor", "作者")
        repository.appendBlockLog(entry)
        val logs = repository.loadBlockLogs().logs
        assertEquals(1, logs.size)
        assertEquals("标题", logs[0].title)
    }

    @Test
    fun `appendBlockLog increments keyword block count`() {
        repository.addKeyword("计数词", "rumor")
        val entry = BlockLogEntry("log_1", "2026-08-11T10:00:00", "标题", "计数词", "rumor", "作者")
        repository.appendBlockLog(entry)
        val kw = repository.loadKeywords().keywords.first { it.word == "计数词" }
        assertEquals(1, kw.blockCount)
    }

    @Test
    fun `exportBackup contains all data`() {
        repository.addKeyword("kw1", "rumor")
        val backup = repository.exportBackup("1.0.0")
        assertTrue(backup.keywords.any { it.word == "kw1" })
        assertEquals("1.0.0", backup.appVersion)
    }

    @Test
    fun `keywordCount returns correct count`() {
        repository.addKeyword("kw1", "rumor")
        repository.addKeyword("kw2", "clickbait")
        assertTrue(repository.keywordCount() >= 2)
    }

    @Test
    fun `blacklistCount returns correct count`() {
        repository.saveBlacklist(BlacklistStore(updatedAt = "2026-08-11", accounts = listOf(
            BlacklistAccount("bl_1", "a", "2026-08-11"),
            BlacklistAccount("bl_2", "b", "2026-08-11")
        )))
        assertEquals(2, repository.blacklistCount())
    }

    @Test
    fun `whitelistCount returns correct count`() {
        repository.saveWhitelist(WhitelistStore(updatedAt = "2026-08-11", entries = listOf(
            WhitelistEntry("wl_1", "keyword", "v1", "2026-08-11")
        )))
        assertEquals(1, repository.whitelistCount())
    }

    @Test
    fun `importBackup merges new keywords and skips duplicates`() {
        repository.addKeyword("已有词", "rumor")
        val backup = BackupFile(
            exportedAt = "2026-08-11T10:00:00",
            appVersion = "1.0.0",
            keywords = listOf(
                KeywordEntry("kw_dup", "已有词", "clickbait", "custom", 0, "2026-08-11T10:00:00"),
                KeywordEntry("kw_new", "新词", "rumor", "custom", 0, "2026-08-11T10:00:00")
            ),
            blacklist = emptyList(),
            whitelist = emptyList()
        )

        val result = repository.importBackup(backup)

        assertTrue(result.success)
        assertEquals(1, result.addedKeywords)
        assertTrue(repository.loadKeywords().keywords.any { it.word == "新词" })
        assertFalse(repository.loadKeywords().keywords.any { it.id == "kw_dup" })
    }

    @Test
    fun `parseBackupJson rejects invalid json`() {
        runCatching { repository.parseBackupJson("{") }
            .onSuccess { error("expected failure") }
            .onFailure { assertTrue(true) }
    }
}
