package com.yinling.guard.data

import android.content.Context
import com.yinling.guard.core.family.FamilyManager
import com.yinling.guard.core.storage.GuardRepository
import com.yinling.guard.core.ui.HomePresenter
import com.yinling.guard.core.ui.RecordsPresenter
import com.yinling.guard.core.ui.SettingsPresenter
import java.io.File
import java.io.InputStream

class AndroidFileReader(
    private val filesDir: File,
    private val assetOpener: (String) -> InputStream
) : com.yinling.guard.core.storage.FileReader {
    override fun exists(path: String): Boolean = File(filesDir, path).exists()

    override fun readText(path: String): String = File(filesDir, path).readText(Charsets.UTF_8)

    override fun writeText(path: String, content: String) {
        val file = File(filesDir, path)
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    override fun readAsset(assetName: String): InputStream? =
        runCatching { assetOpener(assetName) }.getOrNull()
}

object ServiceLocator {
    private lateinit var appContext: Context

    val repository: GuardRepository by lazy {
        GuardRepository(AndroidFileReader(appContext.filesDir) { name ->
            appContext.assets.open(name)
        }).also { repo ->
            repo.loadKeywords()
        }
    }

    val familyManager: FamilyManager by lazy { FamilyManager(repository) }
    val homePresenter: HomePresenter by lazy { HomePresenter(repository) }
    val recordsPresenter: RecordsPresenter by lazy { RecordsPresenter(repository) }
    val settingsPresenter: SettingsPresenter by lazy { SettingsPresenter(repository) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun repository(context: Context = appContext): GuardRepository {
        if (!this::appContext.isInitialized) init(context)
        return repository
    }
}
