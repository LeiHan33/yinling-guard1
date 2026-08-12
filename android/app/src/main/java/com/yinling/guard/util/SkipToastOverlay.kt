package com.yinling.guard.util

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.yinling.guard.R

class SkipToastOverlay(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var hideRunnable: Runnable? = null

    fun show(message: String) {
        handler.post {
            dismiss()
            val view = LayoutInflater.from(service).inflate(R.layout.overlay_skip_toast, null)
            view.findViewById<TextView>(R.id.toast_text).text = message

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = (service.resources.displayMetrics.density * 120).toInt()
            }

            windowManager.addView(view, params)
            overlayView = view

            val runnable = Runnable { dismiss() }
            hideRunnable = runnable
            handler.postDelayed(runnable, DISMISS_MS)
        }
    }

    fun dismiss() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = null
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // View already removed.
            }
            overlayView = null
        }
    }

    companion object {
        private const val DISMISS_MS = 2000L
    }
}
