package com.yinling.guard.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yinling.guard.R
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.databinding.FragmentSettingsBinding
import com.yinling.guard.ui.common.PasswordDialog

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var suppressSwitchCallback = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        render()
        binding.guardSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallback) return@setOnCheckedChangeListener
            val presenter = ServiceLocator.settingsPresenter
            val current = presenter.buildState()
            if (!isChecked && current.guardEnabled) {
                suppressSwitchCallback = true
                binding.guardSwitch.isChecked = true
                suppressSwitchCallback = false
                promptPassword(onSuccess = { presenter.setGuardEnabled(false); render() })
            } else if (isChecked) {
                presenter.setGuardEnabled(true)
            }
        }
        binding.toastSwitch.setOnCheckedChangeListener { _, isChecked ->
            ServiceLocator.settingsPresenter.setToastEnabled(isChecked)
        }
        binding.familyButton.setOnClickListener {
            val state = ServiceLocator.settingsPresenter.buildState()
            if (!state.hasFamilyPassword) {
                promptSetPassword { openFamily() }
            } else {
                promptPassword(onSuccess = { openFamily() })
            }
        }
        binding.helpButton.setOnClickListener {
            findNavController().navigate(R.id.helpFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val state = ServiceLocator.settingsPresenter.buildState()
        suppressSwitchCallback = true
        binding.guardSwitch.isChecked = state.guardEnabled
        suppressSwitchCallback = false
        binding.toastSwitch.isChecked = state.toastEnabled
        binding.platformText.text = "守护平台：${state.targetApp}"
        binding.filterModeText.text = "过滤模式：${state.filterMode}"
        binding.versionText.text = "版本 v${state.appVersion}"
    }

    private fun openFamily() {
        findNavController().navigate(R.id.familyHomeFragment)
    }

    private fun promptPassword(onSuccess: () -> Unit, onCancel: (() -> Unit)? = null) {
        PasswordDialog.show(
            fragment = this,
            title = getString(R.string.password_title),
            onConfirm = { password, showError ->
                if (ServiceLocator.familyManager.verifyPassword(password).success) {
                    onSuccess()
                    true
                } else {
                    showError(getString(R.string.password_error))
                    false
                }
            },
            onCancel = onCancel
        )
    }

    private fun promptSetPassword(onSuccess: () -> Unit) {
        PasswordDialog.show(
            fragment = this,
            title = getString(R.string.password_set_title),
            description = getString(R.string.password_set_desc),
            onConfirm = { password, showError ->
                val result = ServiceLocator.familyManager.setupPassword(password)
                if (result.success) {
                    onSuccess()
                    true
                } else {
                    showError(result.message)
                    false
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
