package com.yinling.guard.ui.family

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.yinling.guard.R
import com.yinling.guard.core.model.KeywordCategory
import com.yinling.guard.core.model.KeywordEntry
import com.yinling.guard.core.model.WhitelistEntry
import com.yinling.guard.core.model.WhitelistType
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.databinding.FragmentBlacklistBinding
import com.yinling.guard.databinding.FragmentFamilyManageBinding
import com.yinling.guard.databinding.FragmentKeywordsBinding
import com.yinling.guard.databinding.FragmentWhitelistBinding
import com.yinling.guard.databinding.ItemKeywordBinding
import com.yinling.guard.service.GuardAccessibilityService
import com.yinling.guard.util.BackupExporter

class FamilyHomeFragment : Fragment() {
    private var _binding: FragmentFamilyManageBinding? = null
    private val binding get() = _binding!!

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        importBackupFromUri(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFamilyManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderCounts()
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.cardKeywords.setOnClickListener { navigateIfPossible(R.id.action_family_to_keywords) }
        binding.cardBlacklist.setOnClickListener { navigateIfPossible(R.id.action_family_to_blacklist) }
        binding.cardWhitelist.setOnClickListener { navigateIfPossible(R.id.action_family_to_whitelist) }
        binding.cardImport.setOnClickListener { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        binding.cardExport.setOnClickListener { exportBackup() }
    }

    override fun onResume() {
        super.onResume()
        renderCounts()
    }

    private fun renderCounts() {
        val repo = ServiceLocator.repository(requireContext())
        binding.tvKeywordCount.text = repo.keywordCount().toString()
        binding.tvBlacklistCount.text = repo.blacklistCount().toString()
        binding.tvWhitelistCount.text = repo.whitelistCount().toString()
    }

    private fun exportBackup() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE)
                return
            }
        }
        performExport()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            performExport()
        }
    }

    private fun performExport() {
        runCatching {
            val config = ServiceLocator.repository(requireContext()).loadConfig()
            val backup = ServiceLocator.familyManager.exportBackup(config.appVersion)
            val file = BackupExporter.exportToDownloads(requireContext(), backup)
            Toast.makeText(requireContext(), getString(R.string.export_success, file.name), Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(requireContext(), it.message ?: "导出失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importBackupFromUri(uri: Uri) {
        runCatching {
            val json = requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: throw IllegalArgumentException("invalid")
            val backup = ServiceLocator.familyManager.parseBackupJson(json).getOrThrow()
            val result = ServiceLocator.familyManager.importBackup(backup)
            GuardAccessibilityService.reloadRules()
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            renderCounts()
        }.onFailure {
            Toast.makeText(requireContext(), getString(R.string.import_invalid), Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateIfPossible(actionId: Int) {
        if (!isAdded) return
        val navController = findNavController()
        if (navController.currentDestination?.getAction(actionId) != null) {
            navController.navigate(actionId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_WRITE_STORAGE = 1001
    }
}

class KeywordListFragment : Fragment() {
    private var _binding: FragmentKeywordsBinding? = null
    private val binding get() = _binding!!
    private var currentCategory: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKeywordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.rvKeywords.layoutManager = LinearLayoutManager(requireContext())
        setupCategoryChips()
        binding.fabAdd.setOnClickListener {
            AddKeywordDialog.show(this) { renderList() }
        }
        renderList()
    }

    override fun onResume() {
        super.onResume()
        renderList()
    }

    private fun setupCategoryChips() {
        binding.chipAll.setOnClickListener {
            currentCategory = null
            binding.chipAll.isChecked = true
            renderList()
        }
        KeywordCategory.entries.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.displayName
                isCheckable = true
                setOnClickListener {
                    currentCategory = category.value
                    binding.chipAll.isChecked = false
                    renderList()
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun renderList() {
        val keywords = ServiceLocator.familyManager.filterKeywords(currentCategory)
        binding.rvKeywords.adapter = KeywordAdapter(keywords) { id ->
            ServiceLocator.familyManager.removeKeyword(id)
            GuardAccessibilityService.reloadRules()
            renderList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class KeywordAdapter(
        private val items: List<KeywordEntry>,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<KeywordAdapter.Holder>() {
        class Holder(val binding: ItemKeywordBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemKeywordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.binding.tvWord.text = item.word
            holder.binding.tvCategory.text = KeywordCategory.fromValue(item.category)?.displayName ?: item.category
            holder.binding.tvBlockCount.text = "拦截 ${item.blockCount} 次"
            holder.binding.btnDelete.setOnClickListener { onDelete(item.id) }
        }

        override fun getItemCount(): Int = items.size
    }
}

class BlacklistFragment : Fragment() {
    private var _binding: FragmentBlacklistBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBlacklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.rvBlacklist.layoutManager = LinearLayoutManager(requireContext())
        binding.fabAdd.setOnClickListener { showAddDialog() }
        renderList()
    }

    override fun onResume() {
        super.onResume()
        renderList()
    }

    private fun renderList() {
        val accounts = ServiceLocator.repository(requireContext()).loadBlacklist().accounts
        binding.tvEmpty.visibility = if (accounts.isEmpty()) View.VISIBLE else View.GONE
        binding.rvBlacklist.visibility = if (accounts.isEmpty()) View.GONE else View.VISIBLE
        binding.rvBlacklist.adapter = BlacklistAdapter(accounts) { id ->
            ServiceLocator.familyManager.removeBlacklistAuthor(id)
            GuardAccessibilityService.reloadRules()
            renderList()
        }
    }

    private fun showAddDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.blacklist_add_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.blacklist_add))
            .setView(input)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                ServiceLocator.familyManager.addBlacklistAuthor(input.text.toString())
                GuardAccessibilityService.reloadRules()
                renderList()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class BlacklistAdapter(
        private val items: List<com.yinling.guard.core.model.BlacklistAccount>,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<BlacklistAdapter.Holder>() {
        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_author_name)
            val delete: ImageButton = view.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blacklist, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.name.text = item.authorName
            holder.delete.setOnClickListener { onDelete(item.id) }
        }

        override fun getItemCount(): Int = items.size
    }
}

class WhitelistFragment : Fragment() {
    private var _binding: FragmentWhitelistBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWhitelistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.rvWhitelist.layoutManager = LinearLayoutManager(requireContext())
        binding.fabAdd.setOnClickListener { showAddDialog() }
        renderList()
    }

    override fun onResume() {
        super.onResume()
        renderList()
    }

    private fun renderList() {
        val entries = ServiceLocator.repository(requireContext()).loadWhitelist().entries
        binding.tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding.rvWhitelist.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        binding.rvWhitelist.adapter = WhitelistAdapter(entries) { id ->
            ServiceLocator.familyManager.removeWhitelistEntry(id)
            GuardAccessibilityService.reloadRules()
            renderList()
        }
    }

    private fun showAddDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.whitelist_add))
            .setItems(arrayOf(getString(R.string.whitelist_add_keyword), getString(R.string.whitelist_add_author))) { _, which ->
                val isAuthor = which == 1
                val input = EditText(requireContext()).apply {
                    hint = getString(R.string.whitelist_value_hint)
                }
                AlertDialog.Builder(requireContext())
                    .setTitle(if (isAuthor) getString(R.string.whitelist_add_author) else getString(R.string.whitelist_add_keyword))
                    .setView(input)
                    .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                        val value = input.text.toString().trim()
                        if (value.isEmpty()) return@setPositiveButton
                        if (isAuthor) {
                            ServiceLocator.familyManager.addWhitelistAuthor(value)
                        } else {
                            ServiceLocator.familyManager.addWhitelistKeyword(value)
                        }
                        GuardAccessibilityService.reloadRules()
                        renderList()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class WhitelistAdapter(
        private val items: List<WhitelistEntry>,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<WhitelistAdapter.Holder>() {
        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val value: TextView = view.findViewById(R.id.tv_value)
            val type: TextView = view.findViewById(R.id.tv_type)
            val delete: ImageButton = view.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_whitelist, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.value.text = item.value
            holder.type.text = if (item.type == WhitelistType.AUTHOR.value) {
                holder.itemView.context.getString(R.string.whitelist_type_author)
            } else {
                holder.itemView.context.getString(R.string.whitelist_type_keyword)
            }
            holder.delete.setOnClickListener { onDelete(item.id) }
        }

        override fun getItemCount(): Int = items.size
    }
}
