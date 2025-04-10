package com.example.cardashboardtest.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.cardashboardtest.databinding.FragmentLogsBinding
import com.example.cardashboardtest.model.LogType
import com.example.cardashboardtest.R
import kotlinx.coroutines.launch

class LogsFragment : Fragment() {
    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogsViewModel by viewModels()
    private lateinit var adapter: LogsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilterChips()
        setupDeleteButton()
        observeDeleteSuccess()
        observeLogs()
    }

    private fun setupRecyclerView() {
        adapter = LogsAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@LogsFragment.adapter
        }
    }

    private fun setupFilterChips() {
        binding.filterChipGroup.apply {
            LogType.values().forEach { logType ->
                addView(createFilterChip(logType))
            }
        }
    }

    private fun createFilterChip(logType: LogType): com.google.android.material.chip.Chip {
        return com.google.android.material.chip.Chip(requireContext()).apply {
            text = logType.name
            isCheckable = true
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleFilter(logType, isChecked)
            }
        }
    }

    private fun setupDeleteButton() {
        binding.clearLogsFab.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.clear_logs))
            .setMessage(getString(R.string.clear_logs_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.clearLogs()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun observeDeleteSuccess() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteSuccess.collect { success ->
                if (success == true) {
                    Snackbar.make(binding.root, "All logs deleted", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeLogs() {
        viewModel.logs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
