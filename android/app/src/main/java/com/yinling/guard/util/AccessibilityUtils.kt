package com.yinling.guard.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.yinling.guard.service.GuardAccessibilityService

object AccessibilityUtils {
    fun isServiceEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${GuardAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun isAccessibilityManagerEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.isEnabled
    }

    fun getServiceInfo(context: Context): AccessibilityServiceInfo? {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getInstalledAccessibilityServiceList()
            .firstOrNull { it.id.contains(context.packageName) }
    }
}
