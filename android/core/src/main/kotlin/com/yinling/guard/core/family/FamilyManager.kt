package com.yinling.guard.core.family

import com.yinling.guard.core.model.BlacklistAccount
import com.yinling.guard.core.model.BackupFile
import com.yinling.guard.core.model.KeywordEntry
import com.yinling.guard.core.model.WhitelistEntry
import com.yinling.guard.core.security.PasswordHasher
import com.yinling.guard.core.storage.GuardRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class FamilyManager(
    private val repository: GuardRepository,
    private val nowProvider: () -> Instant = { Instant.now() }
) {
    data class PasswordSetupResult(val success: Boolean, val message: String)
    data class PasswordVerifyResult(val success: Boolean)

    fun setupPassword(password: String): PasswordSetupResult {
        if (password.length < 4) {
            return PasswordSetupResult(false, "密码至少 4 位")
        }
        val config = repository.loadConfig()
        repository.saveConfig(config.copy(familyPasswordHash = PasswordHasher.hash(password)))
        return PasswordSetupResult(true, "密码设置成功")
    }

    fun verifyPassword(password: String): PasswordVerifyResult {
        val config = repository.loadConfig()
        if (config.familyPasswordHash.isNullOrBlank()) {
            return PasswordVerifyResult(false)
        }
        return PasswordVerifyResult(PasswordHasher.verify(password, config.familyPasswordHash))
    }

    fun canDisableGuard(password: String): Boolean = verifyPassword(password).success

    fun addKeyword(word: String, category: String): Result<KeywordEntry> {
        val trimmed = word.trim()
        if (trimmed.isEmpty() || trimmed.length > 20) {
            return Result.failure(IllegalArgumentException("关键词长度应为 1-20 字"))
        }
        val existing = repository.loadKeywords().keywords.any { it.word.equals(trimmed, ignoreCase = true) }
        if (existing) {
            return Result.failure(IllegalArgumentException("关键词已存在"))
        }
        return Result.success(repository.addKeyword(trimmed, category))
    }

    fun removeKeyword(id: String) {
        repository.removeKeyword(id)
    }

    fun filterKeywords(category: String?): List<KeywordEntry> {
        val all = repository.loadKeywords().keywords
        return if (category.isNullOrBlank() || category == "all") all else all.filter { it.category == category }
    }

    fun addBlacklistAuthor(authorName: String): BlacklistAccount {
        val trimmed = authorName.trim()
        val store = repository.loadBlacklist()
        val account = BlacklistAccount(
            id = "bl_${UUID.randomUUID()}",
            authorName = trimmed,
            addedAt = isoNow()
        )
        repository.saveBlacklist(store.copy(accounts = store.accounts + account, updatedAt = isoNow()))
        return account
    }

    fun removeBlacklistAuthor(id: String) {
        val store = repository.loadBlacklist()
        repository.saveBlacklist(store.copy(accounts = store.accounts.filterNot { it.id == id }, updatedAt = isoNow()))
    }

    fun addWhitelistKeyword(value: String): WhitelistEntry {
        val entry = WhitelistEntry("wl_${UUID.randomUUID()}", "keyword", value.trim(), isoNow())
        val store = repository.loadWhitelist()
        repository.saveWhitelist(store.copy(entries = store.entries + entry, updatedAt = isoNow()))
        return entry
    }

    fun addWhitelistAuthor(value: String): WhitelistEntry {
        val entry = WhitelistEntry("wl_${UUID.randomUUID()}", "author", value.trim(), isoNow())
        val store = repository.loadWhitelist()
        repository.saveWhitelist(store.copy(entries = store.entries + entry, updatedAt = isoNow()))
        return entry
    }

    fun removeWhitelistEntry(id: String) {
        val store = repository.loadWhitelist()
        repository.saveWhitelist(store.copy(entries = store.entries.filterNot { it.id == id }, updatedAt = isoNow()))
    }

    fun exportBackup(appVersion: String) = repository.exportBackup(appVersion)

    fun importBackup(backup: BackupFile) = repository.importBackup(backup)

    fun parseBackupJson(json: String): Result<BackupFile> = runCatching { repository.parseBackupJson(json) }

    private fun isoNow(): String =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(nowProvider().atZone(ZoneId.systemDefault()).toLocalDateTime())
}
