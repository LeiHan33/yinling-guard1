package com.yinling.guard.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yinling.guard.R
import com.yinling.guard.databinding.FragmentOnboardingWelcomeBinding

class OnboardingWelcomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentOnboardingWelcomeBinding.inflate(inflater, container, false)
        binding.startButton.setOnClickListener {
            findNavController().navigate(R.id.onboardingIntroFragment)
        }
        return binding.root
    }
}
