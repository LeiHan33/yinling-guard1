package com.yinling.guard.core.ui

import com.yinling.guard.core.engine.BlockLogFilter
import com.yinling.guard.core.engine.HomeStatsCalculator
import com.yinling.guard.core.model.AppConfig
import com.yinling.guard.core.model.BlockLogEntry
import com.yinling.guard.core.storage.GuardRepository

data class HomeUiState(
    val greeting: String,
    val guardEnabled: Boolean,
    val accessibilityGranted: Boolean,
    val todayBlockCount: Int,
    val totalGuardDays: Int,
    val recentBlocks: List<BlockLogEntry>,
    val targetAppLabel: String = "抖音"
)

data class RecordsUiState(
    val range: BlockLogFilter.Range,
    val logs: List<BlockLogEntry>
)

class HomePresenter(
    private val repository: GuardRepository,
    private val statsCalculator: HomeStatsCalculator = HomeStatsCalculator()
) {
    fun buildState(config: AppConfig, accessibilityGranted: Boolean, nowHour: Int): HomeUiState {
        val logs = repository.loadBlockLogs().logs
        val stats = statsCalculator.calculate(logs, config.firstGuardDate)
        val greetingPrefix = when (nowHour) {
            in 5..11 -> "上午好"
            in 12..17 -> "下午好"
            else -> "晚上好"
        }
        val status = if (config.guardEnabled) "守护中" else "已关闭"
        return HomeUiState(
            greeting = "$greetingPrefix，$status",
            guardEnabled = config.guardEnabled,
            accessibilityGranted = accessibilityGranted,
            todayBlockCount = stats.todayBlockCount,
            totalGuardDays = stats.totalGuardDays,
            recentBlocks = stats.recentBlocks
        )
    }
}

class RecordsPresenter(
    private val repository: GuardRepository,
    private val filter: BlockLogFilter = BlockLogFilter()
) {
    fun buildState(range: BlockLogFilter.Range): RecordsUiState {
        val logs = filter.filter(repository.loadBlockLogs().logs, range)
        return RecordsUiState(range, logs)
    }
}

class SettingsPresenter(private val repository: GuardRepository) {
    data class SettingsUiState(
        val guardEnabled: Boolean,
        val toastEnabled: Boolean,
        val filterMode: String,
        val targetApp: String,
        val appVersion: String,
        val hasFamilyPassword: Boolean
    )

    fun buildState(): SettingsUiState {
        val config = repository.loadConfig()
        return SettingsUiState(
            guardEnabled = config.guardEnabled,
            toastEnabled = config.toastEnabled,
            filterMode = "严格",
            targetApp = "抖音",
            appVersion = config.appVersion,
            hasFamilyPassword = !config.familyPasswordHash.isNullOrBlank()
        )
    }

    fun setGuardEnabled(enabled: Boolean) {
        val config = repository.loadConfig()
        repository.saveConfig(
            config.copy(
                guardEnabled = enabled,
                firstGuardDate = config.firstGuardDate ?: java.time.LocalDate.now().toString()
            )
        )
    }

    fun setToastEnabled(enabled: Boolean) {
        val config = repository.loadConfig()
        repository.saveConfig(config.copy(toastEnabled = enabled))
    }
}
