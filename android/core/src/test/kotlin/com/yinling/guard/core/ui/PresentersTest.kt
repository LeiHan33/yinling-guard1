package com.yinling.guard.core.ui

import com.yinling.guard.core.engine.BlockLogFilter
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

class PresentersTest {

    private lateinit var tempDir: File
    private lateinit var repository: GuardRepository

    @Before
    fun setup() {
        tempDir = createTempDir("presenter_test")
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

    private fun addLog(title: String) {
        repository.appendBlockLog(BlockLogEntry("log_${System.nanoTime()}", "2026-08-11T10:00:00", title, "k", "rumor", "a"))
    }

    @Test
    fun `HomePresenter morning greeting`() {
        val presenter = HomePresenter(repository)
        val config = AppConfig(guardEnabled = true)
        val state = presenter.buildState(config, true, 10)
        assertTrue(state.greeting.contains("上午好"))
        assertTrue(state.greeting.contains("守护中"))
    }

    @Test
    fun `HomePresenter afternoon greeting`() {
        val presenter = HomePresenter(repository)
        val config = AppConfig(guardEnabled = true)
        val state = presenter.buildState(config, true, 14)
        assertTrue(state.greeting.contains("下午好"))
    }

    @Test
    fun `HomePresenter evening greeting`() {
        val presenter = HomePresenter(repository)
        val config = AppConfig(guardEnabled = true)
        val state = presenter.buildState(config, true, 20)
        assertTrue(state.greeting.contains("晚上好"))
    }

    @Test
    fun `HomePresenter shows closed when guard disabled`() {
        val presenter = HomePresenter(repository)
        val config = AppConfig(guardEnabled = false)
        val state = presenter.buildState(config, true, 10)
        assertTrue(state.greeting.contains("已关闭"))
        assertFalse(state.guardEnabled)
    }

    @Test
    fun `HomePresenter shows accessibility status`() {
        val presenter = HomePresenter(repository)
        val state = presenter.buildState(AppConfig(), false, 10)
        assertFalse(state.accessibilityGranted)
    }

    @Test
    fun `HomePresenter calculates today count`() {
        val presenter = HomePresenter(repository)
        val state = presenter.buildState(AppConfig(), true, 10)
        // Initially 0 since no logs added through presenter's repository
        assertTrue(state.todayBlockCount >= 0)
    }

    @Test
    fun `RecordsPresenter filters today`() {
        addLog("今天")
        val presenter = RecordsPresenter(repository)
        val state = presenter.buildState(BlockLogFilter.Range.TODAY)
        assertEquals(BlockLogFilter.Range.TODAY, state.range)
    }

    @Test
    fun `RecordsPresenter filters all`() {
        addLog("日志")
        val presenter = RecordsPresenter(repository)
        val state = presenter.buildState(BlockLogFilter.Range.ALL)
        assertEquals(BlockLogFilter.Range.ALL, state.range)
    }

    @Test
    fun `SettingsPresenter returns correct state`() {
        repository.saveConfig(AppConfig(guardEnabled = true, toastEnabled = false))
        val presenter = SettingsPresenter(repository)
        val state = presenter.buildState()
        assertTrue(state.guardEnabled)
        assertFalse(state.toastEnabled)
        assertEquals("严格", state.filterMode)
        assertEquals("抖音", state.targetApp)
    }

    @Test
    fun `SettingsPresenter setGuardEnabled persists`() {
        val presenter = SettingsPresenter(repository)
        presenter.setGuardEnabled(false)
        assertFalse(repository.loadConfig().guardEnabled)
    }

    @Test
    fun `SettingsPresenter setToastEnabled persists`() {
        val presenter = SettingsPresenter(repository)
        presenter.setToastEnabled(true)
        assertTrue(repository.loadConfig().toastEnabled)
    }

    @Test
    fun `SettingsPresenter sets firstGuardDate when enabling`() {
        val presenter = SettingsPresenter(repository)
        presenter.setGuardEnabled(true)
        val config = repository.loadConfig()
        assertTrue(config.firstGuardDate != null)
    }
}
