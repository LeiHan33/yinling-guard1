package com.yinling.guard.ui.records

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.yinling.guard.R
import com.yinling.guard.core.engine.BlockLogFilter
import com.yinling.guard.core.model.BlockLogEntry
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.databinding.FragmentRecordsBinding

class RecordsFragment : Fragment() {
    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!
    private var currentRange = BlockLogFilter.Range.TODAY
    private var filtersInitialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        if (!filtersInitialized) {
            setupFilters()
            filtersInitialized = true
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun setupFilters() {
        val labels = listOf(
            "今天" to BlockLogFilter.Range.TODAY,
            "昨天" to BlockLogFilter.Range.YESTERDAY,
            "全部" to BlockLogFilter.Range.ALL
        )
        labels.forEach { (label, range) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = range == currentRange
                setOnClickListener {
                    currentRange = range
                    render()
                }
            }
            binding.rangeGroup.addView(chip)
        }
    }

    private fun render() {
        val state = ServiceLocator.recordsPresenter.buildState(currentRange)
        binding.emptyText.visibility = if (state.logs.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (state.logs.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerView.adapter = BlockLogAdapter(state.logs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class BlockLogAdapter(private val items: List<BlockLogEntry>) :
        RecyclerView.Adapter<BlockLogAdapter.Holder>() {
        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tv_title)
            val time: TextView = view.findViewById(R.id.tv_time)
            val keyword: TextView = view.findViewById(R.id.tv_keyword)
            val category: TextView = view.findViewById(R.id.tv_category)
            val author: TextView = view.findViewById(R.id.tv_author)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_block_log, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context
            holder.title.text = item.title
            holder.time.text = CategoryUi.formatTime(item.timestamp)
            holder.keyword.text = item.keyword
            CategoryUi.applyCategoryTag(context, holder.category, item.category)
            holder.author.text = item.author.ifBlank { "未知作者" }
        }

        override fun getItemCount(): Int = items.size
    }
}
