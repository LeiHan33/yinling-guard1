package com.yinling.guard.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yinling.guard.R
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.databinding.FragmentHomeBinding
import com.yinling.guard.util.AccessibilityUtils
import java.util.Calendar

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val config = ServiceLocator.repository(requireContext()).loadConfig()
        val granted = AccessibilityUtils.isServiceEnabled(requireContext())
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val state = ServiceLocator.homePresenter.buildState(config, granted, hour)

        binding.greetingText.text = state.greeting
        binding.todayCountText.text = "今日拦截\n${state.todayBlockCount}"
        binding.guardDaysText.text = "累计守护\n${state.totalGuardDays}天"

        binding.statusText.text = when {
            !state.accessibilityGranted -> getString(R.string.permission_error)
            state.guardEnabled -> getString(R.string.guard_on)
            else -> getString(R.string.guard_off)
        }

        binding.statusCard.setOnClickListener {
            if (!state.accessibilityGranted) {
                AccessibilityUtils.openAccessibilitySettings(requireContext())
            }
        }

        binding.viewAllText.setOnClickListener {
            findNavController().navigate(R.id.recordsFragment)
        }

        binding.recentContainer.removeAllViews()
        if (state.recentBlocks.isEmpty()) {
            addRecentLine("暂无拦截")
        } else {
            state.recentBlocks.forEach { entry ->
                addRecentLine("${entry.title}  [${entry.keyword}]")
            }
        }
    }

    private fun addRecentLine(text: String) {
        val tv = TextView(requireContext()).apply {
            this.text = text
            setPadding(0, 12, 0, 12)
        }
        binding.recentContainer.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
