package com.yinling.guard.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.yinling.guard.R
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.databinding.FragmentOnboardingDoneBinding
import com.yinling.guard.databinding.FragmentOnboardingIntroBinding
import com.yinling.guard.databinding.FragmentOnboardingPermissionBinding
import com.yinling.guard.databinding.ItemOnboardingFeatureBinding
import com.yinling.guard.util.AccessibilityUtils
import java.time.LocalDate

class OnboardingIntroFragment : Fragment() {
    private var _binding: FragmentOnboardingIntroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val features = listOf(
            Triple("🔍", getString(R.string.onboarding_feature_detect_title), getString(R.string.onboarding_feature_detect_desc)),
            Triple("⏭️", getString(R.string.onboarding_feature_skip_title), getString(R.string.onboarding_feature_skip_desc)),
            Triple("👨‍👩‍👧", getString(R.string.onboarding_feature_family_title), getString(R.string.onboarding_feature_family_desc))
        )
        features.forEach { (icon, title, desc) ->
            addFeatureCard(icon, title, desc)
        }
        binding.nextButton.setOnClickListener {
            findNavController().navigate(R.id.onboardingPermissionFragment)
        }
    }

    private fun addFeatureCard(icon: String, title: String, desc: String) {
        val itemBinding = ItemOnboardingFeatureBinding.inflate(layoutInflater, binding.featureContainer, false)
        itemBinding.featureIcon.text = icon
        itemBinding.featureTitle.text = title
        itemBinding.featureDesc.text = desc
        binding.featureContainer.addView(itemBinding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class OnboardingPermissionFragment : Fragment() {
    private var _binding: FragmentOnboardingPermissionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.openPermissionButton.setOnClickListener {
            AccessibilityUtils.openAccessibilitySettings(requireContext())
        }
        binding.continueButton.setOnClickListener { navigateToDone() }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState(autoAdvance = true)
    }

    private fun refreshPermissionState(autoAdvance: Boolean) {
        val granted = AccessibilityUtils.isServiceEnabled(requireContext())
        binding.statusText.text = if (granted) {
            getString(R.string.onboarding_permission_granted)
        } else {
            getString(R.string.onboarding_permission_desc)
        }
        binding.continueButton.isEnabled = granted
        if (granted && autoAdvance) {
            navigateToDone()
        }
    }

    private fun navigateToDone() {
        if (!AccessibilityUtils.isServiceEnabled(requireContext())) return
        findNavController().navigate(R.id.onboardingDoneFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class OnboardingDoneFragment : Fragment() {
    private var _binding: FragmentOnboardingDoneBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingDoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.retryButton.setOnClickListener {
            AccessibilityUtils.openAccessibilitySettings(requireContext())
        }
        binding.enterButton.setOnClickListener { completeOnboarding() }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val granted = AccessibilityUtils.isServiceEnabled(requireContext())
        binding.statusText.text = if (granted) {
            getString(R.string.onboarding_done_success)
        } else {
            getString(R.string.onboarding_done_pending)
        }
        binding.iconBadge.text = if (granted) "✅" else "⚠️"
        binding.enterButton.isEnabled = granted
        binding.retryButton.visibility = if (granted) View.GONE else View.VISIBLE
    }

    private fun completeOnboarding() {
        if (!AccessibilityUtils.isServiceEnabled(requireContext())) return

        val repo = ServiceLocator.repository(requireContext())
        var config = repo.loadConfig().copy(onboardingCompleted = true, guardEnabled = true)
        if (config.firstGuardDate == null) {
            config = config.copy(firstGuardDate = LocalDate.now().toString())
        }
        repo.saveConfig(config)

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.homeFragment, null, navOptions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
