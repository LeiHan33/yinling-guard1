package com.yinling.guard.core.family

import com.yinling.guard.core.model.*
import com.yinling.guard.core.storage.FileReader
import com.yinling.guard.core.storage.GuardRepository
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FamilyManagerTest {

    private lateinit var tempDir: File
    private lateinit var repository: GuardRepository
    private lateinit var manager: FamilyManager

    @Before
    fun setup() {
        tempDir = createTempDir("family_test")
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
        manager = FamilyManager(repository, nowProvider = { Instant.parse("2026-08-11T12:00:00Z") })
    }

    // ========== 密码管理 ==========

    @Test
    fun `setup password with valid password succeeds`() {
        val result = manager.setupPassword("1234")
        assertTrue(result.success)
    }

    @Test
    fun `setup password with too short password fails`() {
        val result = manager.setupPassword("123")
        assertFalse(result.success)
        assertTrue(result.message.contains("4"))
    }

    @Test
    fun `verify password returns true for correct password`() {
        manager.setupPassword("mypassword")
        val result = manager.verifyPassword("mypassword")
        assertTrue(result.success)
    }

    @Test
    fun `verify password returns false for wrong password`() {
        manager.setupPassword("mypassword")
        val result = manager.verifyPassword("wrong")
        assertFalse(result.success)
    }

    @Test
    fun `verify password returns false when no password set`() {
        val result = manager.verifyPassword("anything")
        assertFalse(result.success)
    }

    @Test
    fun `can disable guard with correct password`() {
        manager.setupPassword("1234")
        assertTrue(manager.canDisableGuard("1234"))
    }

    @Test
    fun `cannot disable guard with wrong password`() {
        manager.setupPassword("1234")
        assertFalse(manager.canDisableGuard("5678"))
    }

    // ========== 关键词管理 ==========

    @Test
    fun `add keyword succeeds`() {
        val result = manager.addKeyword("测试词", "rumor")
        assertTrue(result.isSuccess)
        assertEquals("测试词", result.getOrNull()?.word)
    }

    @Test
    fun `add keyword with empty word fails`() {
        val result = manager.addKeyword("", "rumor")
        assertTrue(result.isFailure)
    }

    @Test
    fun `add keyword with word over 20 chars fails`() {
        val result = manager.addKeyword("a".repeat(21), "rumor")
        assertTrue(result.isFailure)
    }

    @Test
    fun `add keyword with duplicate word fails`() {
        manager.addKeyword("重复词", "rumor")
        val result = manager.addKeyword("重复词", "clickbait")
        assertTrue(result.isFailure)
    }

    @Test
    fun `remove keyword removes from repository`() {
        val added = manager.addKeyword("要删的词", "rumor").getOrNull()!!
        manager.removeKeyword(added.id)
        assertFalse(repository.loadKeywords().keywords.any { it.id == added.id })
    }

    @Test
    fun `filter keywords returns all when category is null`() {
        manager.addKeyword("词1", "rumor")
        manager.addKeyword("词2", "clickbait")
        val all = manager.filterKeywords(null)
        assertTrue(all.size >= 2)
    }

    @Test
    fun `filter keywords by category`() {
        manager.addKeyword("谣言词", "rumor")
        manager.addKeyword("标题党词", "clickbait")
        val rumors = manager.filterKeywords("rumor")
        assertTrue(rumors.any { it.word == "谣言词" })
        assertFalse(rumors.any { it.word == "标题党词" })
    }

    @Test
    fun `filter keywords with all returns everything`() {
        manager.addKeyword("词1", "rumor")
        val all = manager.filterKeywords("all")
        assertTrue(all.isNotEmpty())
    }

    // ========== 黑名单管理 ==========

    @Test
    fun `add blacklist author succeeds`() {
        val account = manager.addBlacklistAuthor("坏人")
        assertEquals("坏人", account.authorName)
    }

    @Test
    fun `add blacklist author persists`() {
        manager.addBlacklistAuthor("坏人")
        val accounts = repository.loadBlacklist().accounts
        assertTrue(accounts.any { it.authorName == "坏人" })
    }

    @Test
    fun `remove blacklist author removes from repository`() {
        val added = manager.addBlacklistAuthor("要删")
        manager.removeBlacklistAuthor(added.id)
        assertFalse(repository.loadBlacklist().accounts.any { it.id == added.id })
    }

    @Test
    fun `add multiple blacklist authors`() {
        manager.addBlacklistAuthor("人1")
        manager.addBlacklistAuthor("人2")
        val accounts = repository.loadBlacklist().accounts
        assertTrue(accounts.size >= 2)
    }

    // ========== 白名单管理 ==========

    @Test
    fun `add whitelist keyword succeeds`() {
        val entry = manager.addWhitelistKeyword("官方发布")
        assertEquals("官方发布", entry.value)
        assertEquals("keyword", entry.type)
    }

    @Test
    fun `add whitelist keyword persists`() {
        manager.addWhitelistKeyword("官方")
        val entries = repository.loadWhitelist().entries
        assertTrue(entries.any { it.value == "官方" })
    }

    // ========== 导出 ==========

    @Test
    fun `export backup contains keywords`() {
        manager.addKeyword("kw1", "rumor")
        val backup = manager.exportBackup("1.0.0")
        assertTrue(backup.keywords.any { it.word == "kw1" })
    }

    @Test
    fun `export backup contains blacklist`() {
        manager.addBlacklistAuthor("bl1")
        val backup = manager.exportBackup("1.0.0")
        assertTrue(backup.blacklist.any { it.authorName == "bl1" })
    }
}
