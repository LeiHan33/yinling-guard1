package com.yinling.guard.testing

import android.content.Context
import com.yinling.guard.core.model.AppConfig
import com.yinling.guard.core.model.BlockLogEntry
import com.yinling.guard.data.ServiceLocator
import java.time.LocalDate

object TestFixtures {
    fun setup(context: Context) {
        ServiceLocator.init(context.applicationContext)
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        context.filesDir.mkdirs()
    }

    fun saveConfig(context: Context, config: AppConfig = AppConfig()) {
        ServiceLocator.repository(context).saveConfig(config)
    }

    fun seedBlockLog(
        context: Context,
        title: String = "震惊！某国即将崩溃",
        keyword: String = "崩溃",
        category: String = "incitement",
        author: String = "测试作者"
    ) {
        val today = LocalDate.now().toString()
        ServiceLocator.repository(context).appendBlockLog(
            BlockLogEntry(
                id = "log_test_${System.nanoTime()}",
                timestamp = "${today}T14:32:00",
                title = title,
                keyword = keyword,
                category = category,
                author = author
            )
        )
    }
}
