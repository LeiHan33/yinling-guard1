package com.yinling.guard.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yinling.guard.R
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.databinding.FragmentSettingsBinding

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
        binding.versionText.text = "版本 v${state.appVersion}"
    }

    private fun openFamily() {
        findNavController().navigate(R.id.familyHomeFragment)
    }

    private fun promptPassword(onSuccess: () -> Unit, onCancel: (() -> Unit)? = null) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("输入子女密码")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                if (ServiceLocator.familyManager.verifyPassword(input.text.toString()).success) {
                    onSuccess()
                } else {
                    android.widget.Toast.makeText(requireContext(), "密码错误", android.widget.Toast.LENGTH_SHORT).show()
                    onCancel?.invoke()
                }
            }
            .setNegativeButton("取消") { _, _ -> onCancel?.invoke() }
            .show()
    }

    private fun promptSetPassword(onSuccess: () -> Unit) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("设置子女密码")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val result = ServiceLocator.familyManager.setupPassword(input.text.toString())
                android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                if (result.success) onSuccess()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
