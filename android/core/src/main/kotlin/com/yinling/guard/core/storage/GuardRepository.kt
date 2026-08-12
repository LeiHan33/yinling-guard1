package com.yinling.guard.core.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.yinling.guard.core.model.AppConfig
import com.yinling.guard.core.model.BackupFile
import com.yinling.guard.core.model.BlacklistStore
import com.yinling.guard.core.model.BlockLogStore
import com.yinling.guard.core.model.KeywordEntry
import com.yinling.guard.core.model.KeywordStore
import com.yinling.guard.core.model.WhitelistStore
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface FileReader {
    fun exists(path: String): Boolean
    fun readText(path: String): String
    fun writeText(path: String, content: String)
    fun readAsset(assetName: String): InputStream?
}

class LocalFileReader(private val baseDir: File) : FileReader {
    override fun exists(path: String): Boolean = File(baseDir, path).exists()

    override fun readText(path: String): String = File(baseDir, path).readText(Charsets.UTF_8)

    override fun writeText(path: String, content: String) {
        val file = File(baseDir, path)
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    override fun readAsset(assetName: String): InputStream? = null
}

class GuardRepository(
    private val fileReader: FileReader,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create(),
    private val nowProvider: () -> Instant = { Instant.now() }
) {
    fun loadConfig(): AppConfig {
        return if (fileReader.exists(CONFIG_FILE)) {
            gson.fromJson(fileReader.readText(CONFIG_FILE), AppConfig::class.java)
        } else {
            AppConfig()
        }
    }

    fun saveConfig(config: AppConfig) {
        fileReader.writeText(CONFIG_FILE, gson.toJson(config))
    }

    fun loadKeywords(): KeywordStore {
        ensureKeywordsInitialized()
        return gson.fromJson(fileReader.readText(KEYWORDS_FILE), KeywordStore::class.java)
    }

    fun saveKeywords(store: KeywordStore) {
        fileReader.writeText(KEYWORDS_FILE, gson.toJson(store))
    }

    fun loadBlacklist(): BlacklistStore {
        return if (fileReader.exists(BLACKLIST_FILE)) {
            gson.fromJson(fileReader.readText(BLACKLIST_FILE), BlacklistStore::class.java)
        } else {
            BlacklistStore(updatedAt = isoNow(), accounts = emptyList())
        }
    }

    fun saveBlacklist(store: BlacklistStore) {
        fileReader.writeText(BLACKLIST_FILE, gson.toJson(store))
    }

    fun loadWhitelist(): WhitelistStore {
        return if (fileReader.exists(WHITELIST_FILE)) {
            gson.fromJson(fileReader.readText(WHITELIST_FILE), WhitelistStore::class.java)
        } else {
            WhitelistStore(updatedAt = isoNow(), entries = emptyList())
        }
    }

    fun saveWhitelist(store: WhitelistStore) {
        fileReader.writeText(WHITELIST_FILE, gson.toJson(store))
    }

    fun loadBlockLogs(): BlockLogStore {
        return if (fileReader.exists(BLOCK_LOGS_FILE)) {
            gson.fromJson(fileReader.readText(BLOCK_LOGS_FILE), BlockLogStore::class.java)
        } else {
            BlockLogStore(logs = emptyList())
        }
    }

    fun saveBlockLogs(store: BlockLogStore) {
        fileReader.writeText(BLOCK_LOGS_FILE, gson.toJson(store))
    }

    fun appendBlockLog(entry: com.yinling.guard.core.model.BlockLogEntry, retention: com.yinling.guard.core.engine.BlockLogRetention = com.yinling.guard.core.engine.BlockLogRetention()) {
        val current = loadBlockLogs()
        val merged = retention.prune(current.logs + entry)
        saveBlockLogs(BlockLogStore(logs = merged))
        incrementKeywordBlockCount(entry.keyword)
    }

    fun addKeyword(word: String, category: String): KeywordEntry {
        val store = loadKeywords()
        val entry = KeywordEntry(
            id = "kw_${System.currentTimeMillis()}",
            word = word.trim(),
            category = category,
            source = "custom",
            blockCount = 0,
            createdAt = isoNow()
        )
        saveKeywords(store.copy(keywords = store.keywords + entry, updatedAt = isoNow()))
        return entry
    }

    fun removeKeyword(id: String) {
        val store = loadKeywords()
        saveKeywords(store.copy(keywords = store.keywords.filterNot { it.id == id }, updatedAt = isoNow()))
    }

    fun exportBackup(appVersion: String): BackupFile {
        return BackupFile(
            exportedAt = isoNow(),
            appVersion = appVersion,
            keywords = loadKeywords().keywords,
            blacklist = loadBlacklist().accounts,
            whitelist = loadWhitelist().entries
        )
    }

    fun keywordCount(): Int = loadKeywords().keywords.size

    fun blacklistCount(): Int = loadBlacklist().accounts.size

    fun whitelistCount(): Int = loadWhitelist().entries.size

    private fun incrementKeywordBlockCount(keyword: String) {
        val store = loadKeywords()
        val updated = store.keywords.map { entry ->
            if (entry.word.equals(keyword, ignoreCase = true)) entry.copy(blockCount = entry.blockCount + 1) else entry
        }
        saveKeywords(store.copy(keywords = updated, updatedAt = isoNow()))
    }

    private fun ensureKeywordsInitialized() {
        if (!fileReader.exists(KEYWORDS_FILE)) {
            val default = fileReader.readAsset(KEYWORDS_DEFAULT_ASSET)
            if (default != null) {
                fileReader.writeText(KEYWORDS_FILE, default.bufferedReader().readText())
            } else {
                fileReader.writeText(KEYWORDS_FILE, gson.toJson(KeywordStore(updatedAt = isoNow(), keywords = emptyList())))
            }
        }
    }

    private fun isoNow(): String =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(nowProvider().atZone(ZoneId.systemDefault()).toLocalDateTime())

    companion object {
        const val CONFIG_FILE = "config.json"
        const val KEYWORDS_FILE = "keywords.json"
        const val BLACKLIST_FILE = "blacklist.json"
        const val WHITELIST_FILE = "whitelist.json"
        const val BLOCK_LOGS_FILE = "block_logs.json"
        const val KEYWORDS_DEFAULT_ASSET = "keywords_default.json"
    }
}
