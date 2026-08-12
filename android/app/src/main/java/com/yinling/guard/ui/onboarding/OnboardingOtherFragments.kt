package com.yinling.guard.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yinling.guard.R

class OnboardingIntroFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        listOf(
            "自动识别" to "检测视频标题中的谣言、养生骗局关键词",
            "自动跳过" to "发现可疑内容时滑到下一个视频",
            "家人可管理" to "子女可添加新的屏蔽词"
        ).forEach { (title, desc) ->
            layout.addView(TextView(requireContext()).apply {
                text = "$title\n$desc"
                setPadding(0, 0, 0, 24)
            })
        }
        layout.addView(Button(requireContext()).apply {
            text = "下一步"
            setOnClickListener { findNavController().navigate(R.id.onboardingPermissionFragment) }
        })
        return layout
    }
}

class OnboardingPermissionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        layout.addView(TextView(requireContext()).apply {
            text = "需要开启「无障碍服务」才能读取抖音视频标题"
        })
        layout.addView(Button(requireContext()).apply {
            text = "去开启权限"
            setOnClickListener {
                com.yinling.guard.util.AccessibilityUtils.openAccessibilitySettings(requireContext())
            }
        })
        layout.addView(Button(requireContext()).apply {
            text = "我已开启，继续"
            setOnClickListener { findNavController().navigate(R.id.onboardingDoneFragment) }
        })
        return layout
    }
}

class OnboardingDoneFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        layout.addView(TextView(requireContext()).apply {
            text = "守护已开启\n设置完成"
            textSize = 20f
        })
        layout.addView(Button(requireContext()).apply {
            text = "进入首页"
            setOnClickListener {
                val repo = com.yinling.guard.data.ServiceLocator.repository(requireContext())
                val config = repo.loadConfig().copy(onboardingCompleted = true, guardEnabled = true)
                repo.saveConfig(
                    if (config.firstGuardDate == null) config.copy(firstGuardDate = java.time.LocalDate.now().toString())
                    else config
                )
                findNavController().navigate(R.id.homeFragment)
            }
        })
        return layout
    }
}
