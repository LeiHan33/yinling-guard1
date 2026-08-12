package com.yinling.guard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.yinling.guard.R
import com.yinling.guard.core.engine.ContentMatcher
import com.yinling.guard.core.engine.DouyinScreenContext
import com.yinling.guard.core.engine.GuardEngine
import com.yinling.guard.core.model.VideoSnapshot
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.util.DouyinNodeParser
import com.yinling.guard.util.SkipToastOverlay

class GuardAccessibilityService : AccessibilityService() {
    private val guardEngine = GuardEngine()
    private var skipToastOverlay: SkipToastOverlay? = null
    private var lastSignature = ""
    private var lastActionAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var pendingEvaluation: Runnable? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != ContentMatcher.DOUYIN_PACKAGE) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            lastSignature = ""
        }

        scheduleEvaluation()
    }

    private fun scheduleEvaluation() {
        pendingEvaluation?.let { handler.removeCallbacks(it) }
        pendingEvaluation = Runnable { evaluateCurrentWindow() }
        handler.postDelayed(pendingEvaluation!!, EVALUATION_DELAY_MS)
    }

    private fun evaluateCurrentWindow() {
        val root = rootInActiveWindow ?: return
        val parsed = DouyinNodeParser.parse(root)
        if (parsed.screenContext != DouyinScreenContext.VIDEO_FEED) {
            lastSignature = ""
            return
        }

        val signature = "${parsed.title}|${parsed.author}|${parsed.hasCaptionEvidence}"
        if (signature.isBlank() || signature == "|false" || signature == lastSignature) return
        if (parsed.title.isBlank() && parsed.author.isBlank()) return
        lastSignature = signature

        val repo = ServiceLocator.repository(this)
        val config = repo.loadConfig()
        val decision = guardEngine.evaluate(
            VideoSnapshot(
                title = parsed.title,
                author = parsed.author,
                packageName = ContentMatcher.DOUYIN_PACKAGE,
                allText = parsed.allText,
                inFeedContext = true,
                hasCaptionEvidence = parsed.hasCaptionEvidence
            ),
            config.guardEnabled,
            repo.loadKeywords().keywords,
            repo.loadBlacklist().accounts,
            repo.loadWhitelist().entries,
            config.filterMode
        )

        if (!decision.shouldBlock) return

        val now = System.currentTimeMillis()
        if (now - lastActionAt < 1200) return
        lastActionAt = now

        decision.logEntry?.let { repo.appendBlockLog(it) }
        performSwipeUp()
        if (config.toastEnabled) {
            toastOverlay().show(getString(R.string.skip_toast))
        }
    }

    private fun toastOverlay(): SkipToastOverlay =
        skipToastOverlay ?: SkipToastOverlay(this).also { skipToastOverlay = it }

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
        private const val EVALUATION_DELAY_MS = 350L

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
        pendingEvaluation?.let { handler.removeCallbacks(it) }
        pendingEvaluation = null
        skipToastOverlay?.dismiss()
        skipToastOverlay = null
        instance = null
        super.onDestroy()
    }
}
