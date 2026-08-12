package com.yinling.guard.ui.records

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import com.yinling.guard.R
import com.yinling.guard.core.model.KeywordCategory

object CategoryUi {
    fun displayName(category: String): String =
        KeywordCategory.fromValue(category)?.displayName ?: when (category) {
            "blacklist" -> "黑名单"
            else -> category
        }

    fun colorRes(category: String): Int = when (category) {
        KeywordCategory.HEALTH_SCAM.value -> R.color.category_health_scam
        KeywordCategory.RUMOR.value -> R.color.category_rumor
        KeywordCategory.INCITEMENT.value -> R.color.category_incitement
        KeywordCategory.CLICKBAIT.value -> R.color.category_clickbait
        "blacklist" -> R.color.category_blacklist
        else -> R.color.text_secondary
    }

    fun applyCategoryTag(context: Context, view: android.widget.TextView, category: String) {
        view.text = displayName(category)
        view.setTextColor(ContextCompat.getColor(context, R.color.white))
        val background = GradientDrawable().apply {
            cornerRadius = context.resources.displayMetrics.density * 12
            setColor(ContextCompat.getColor(context, colorRes(category)))
        }
        view.background = background
    }

    fun formatTime(timestamp: String): String {
        val timePart = timestamp.substringAfter('T', missingDelimiterValue = "")
        return if (timePart.length >= 5) timePart.substring(0, 5) else timestamp
    }
}
