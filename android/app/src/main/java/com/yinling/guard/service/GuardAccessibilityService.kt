package com.yinling.guard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.yinling.guard.R
import com.yinling.guard.core.engine.ContentMatcher
import com.yinling.guard.core.engine.GuardEngine
import com.yinling.guard.core.model.VideoSnapshot
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.util.DouyinNodeParser

class GuardAccessibilityService : AccessibilityService() {
    private val guardEngine = GuardEngine()
    private val handler = Handler(Looper.getMainLooper())
    private var lastSignature = ""
    private var lastActionAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != ContentMatcher.DOUYIN_PACKAGE) return

        val root = rootInActiveWindow ?: return
        val parsed = DouyinNodeParser.parse(root)
        val signature = "${parsed.title}|${parsed.author}"
        if (signature.isBlank() || signature == "|" || signature == lastSignature) return
        lastSignature = signature

        val repo = ServiceLocator.repository(this)
        val config = repo.loadConfig()
        val decision = guardEngine.evaluate(
            VideoSnapshot(parsed.title, parsed.author, ContentMatcher.DOUYIN_PACKAGE),
            config.guardEnabled,
            repo.loadKeywords().keywords,
            repo.loadBlacklist().accounts,
            repo.loadWhitelist().entries
        )

        if (!decision.shouldBlock) return

        val now = System.currentTimeMillis()
        if (now - lastActionAt < 1200) return
        lastActionAt = now

        decision.logEntry?.let { repo.appendBlockLog(it) }
        performSwipeUp()
        if (config.toastEnabled) {
            handler.post {
                Toast.makeText(applicationContext, getString(R.string.skip_toast), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInterrupt() = Unit

    private fun performSwipeUp() {
        val metrics = resources.displayMetrics
        val startX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.75f
        val endY = metrics.heightPixels * 0.25f
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250))
            .build()
        dispatchGesture(gesture, null, null)
    }

    companion object {
        @Volatile
        var instance: GuardAccessibilityService? = null

        fun reloadRules() {
            instance?.clearSignature()
        }
    }

    fun clearSignature() {
        lastSignature = ""
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
