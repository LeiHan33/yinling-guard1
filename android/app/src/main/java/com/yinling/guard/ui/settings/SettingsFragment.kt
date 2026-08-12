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
            navigateIfPossible(R.id.action_settings_to_help)
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) render()
    }

    private fun render() {
        val binding = _binding ?: return
        val state = ServiceLocator.settingsPresenter.buildState()
        suppressSwitchCallback = true
        binding.guardSwitch.isChecked = state.guardEnabled
        suppressSwitchCallback = false
        binding.toastSwitch.isChecked = state.toastEnabled
        binding.platformText.text = getString(R.string.settings_platform, state.targetApp)
        binding.filterModeText.text = getString(R.string.settings_filter_mode, state.filterMode)
        binding.versionText.text = getString(R.string.settings_version, state.appVersion)
    }

    private fun openFamily() {
        navigateIfPossible(R.id.action_settings_to_family)
    }

    private fun navigateIfPossible(actionId: Int) {
        if (!isAdded) return
        val navController = findNavController()
        if (navController.currentDestination?.getAction(actionId) != null) {
            navController.navigate(actionId)
        }
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
        _binding?.guardSwitch?.setOnCheckedChangeListener(null)
        _binding?.toastSwitch?.setOnCheckedChangeListener(null)
        super.onDestroyView()
        _binding = null
    }
}
