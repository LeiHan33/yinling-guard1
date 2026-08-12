package com.yinling.guard.ui.family

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yinling.guard.R
import com.yinling.guard.core.model.KeywordCategory
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.service.GuardAccessibilityService
import com.yinling.guard.util.BackupExporter

class FamilyHomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val repo = ServiceLocator.repository(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        layout.addView(TextView(requireContext()).apply {
            text = "屏蔽词：${repo.keywordCount()}  黑名单：${repo.blacklistCount()}  白名单：${repo.whitelistCount()}"
        })
        layout.addView(navButton("屏蔽词管理") { findNavController().navigate(R.id.keywordListFragment) })
        layout.addView(navButton("账号黑名单") { findNavController().navigate(R.id.blacklistFragment) })
        layout.addView(navButton("导出词库") {
            val backup = ServiceLocator.familyManager.exportBackup("1.0.0")
            val file = BackupExporter.exportToDownloads(requireContext(), backup)
            Toast.makeText(requireContext(), "已导出：${file.absolutePath}", Toast.LENGTH_LONG).show()
        })
        return layout
    }

    private fun navButton(text: String, action: () -> Unit): Button =
        Button(requireContext()).apply {
            this.text = text
            setOnClickListener { action() }
        }
}

class KeywordListFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        ServiceLocator.familyManager.filterKeywords(null).forEach { entry ->
            layout.addView(TextView(requireContext()).apply {
                text = "${entry.word} (${entry.category})  拦截${entry.blockCount}次"
                setOnLongClickListener {
                    ServiceLocator.familyManager.removeKeyword(entry.id)
                    GuardAccessibilityService.reloadRules()
                    requireActivity().supportFragmentManager.beginTransaction().detach(this@KeywordListFragment).attach(this@KeywordListFragment).commit()
                    true
                }
            })
        }
        layout.addView(Button(requireContext()).apply {
            text = "+ 添加屏蔽词"
            setOnClickListener { findNavController().navigate(R.id.addKeywordFragment) }
        })
        return layout
    }
}

class AddKeywordFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val input = EditText(requireContext()).apply { hint = "输入要屏蔽的词" }
        layout.addView(input)
        var category = KeywordCategory.RUMOR.value
        KeywordCategory.entries.forEach { cat ->
            layout.addView(Button(requireContext()).apply {
                text = cat.displayName
                setOnClickListener { category = cat.value }
            })
        }
        layout.addView(Button(requireContext()).apply {
            text = "确认添加"
            setOnClickListener {
                ServiceLocator.familyManager.addKeyword(input.text.toString(), category)
                    .onSuccess {
                        GuardAccessibilityService.reloadRules()
                        findNavController().popBackStack()
                    }
                    .onFailure {
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                    }
            }
        })
        return layout
    }
}

class BlacklistFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        ServiceLocator.repository(requireContext()).loadBlacklist().accounts.forEach { account ->
            layout.addView(TextView(requireContext()).apply {
                text = account.authorName
                setOnLongClickListener {
                    ServiceLocator.familyManager.removeBlacklistAuthor(account.id)
                    GuardAccessibilityService.reloadRules()
                    requireActivity().supportFragmentManager.beginTransaction().detach(this@BlacklistFragment).attach(this@BlacklistFragment).commit()
                    true
                }
            })
        }
        layout.addView(Button(requireContext()).apply {
            text = "+ 添加账号"
            setOnClickListener {
                val input = EditText(requireContext())
                AlertDialog.Builder(requireContext())
                    .setTitle("添加黑名单账号")
                    .setView(input)
                    .setPositiveButton("添加") { _, _ ->
                        ServiceLocator.familyManager.addBlacklistAuthor(input.text.toString())
                        GuardAccessibilityService.reloadRules()
                        requireActivity().supportFragmentManager.beginTransaction().detach(this@BlacklistFragment).attach(this@BlacklistFragment).commit()
                    }
                    .show()
            }
        })
        return layout
    }
}
