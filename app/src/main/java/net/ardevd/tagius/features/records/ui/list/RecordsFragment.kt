package net.ardevd.tagius.features.records.ui.list

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import net.ardevd.tagius.R
import net.ardevd.tagius.core.network.RetrofitClient
import net.ardevd.tagius.core.utils.DateRanges
import net.ardevd.tagius.databinding.FragmentRecordsListBinding
import net.ardevd.tagius.features.records.data.RecordsRepository
import net.ardevd.tagius.features.records.ui.add.AddRecordBottomSheet
import net.ardevd.tagius.features.records.ui.edit.EditRecordBottomSheet
import net.ardevd.tagius.features.records.viewmodel.RecordsUiState
import net.ardevd.tagius.features.records.viewmodel.RecordsViewModel
import net.ardevd.tagius.features.records.viewmodel.RecordsViewModelFactory

class RecordsListFragment : Fragment(R.layout.fragment_records_list) {

    private var _binding: FragmentRecordsListBinding? = null

    private val binding get() = _binding!!


    private val viewModel: RecordsViewModel by viewModels {
        val apiService = RetrofitClient.getInstance(requireContext())
        val repository = RecordsRepository(apiService)
        RecordsViewModelFactory(repository)
    }

    private val recordsAdapter =
        RecordsAdapter(
            onStopClick = { record -> viewModel.stopRecord(record) },
            onItemClick = { record ->
                val editSheet =
                    EditRecordBottomSheet(record = record, onSave = { newDesc, newStart, newEnd ->

                        viewModel.updateRecord(record, newDesc, newStart, newEnd)
                    }, onDelete = {
                        viewModel.deleteRecord(record)
                    })
                editSheet.show(parentFragmentManager, EditRecordBottomSheet.TAG)
            })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecordsListBinding.bind(view)

        setupRecyclerView()
        observeState()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadRecords()
        }

        // This makes the arrow the Primary color and the circle the Surface color
        val typedValue = android.util.TypedValue()

        // Get colorPrimary
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, typedValue, true
        )
        val colorPrimary = typedValue.data

        // Get colorSurfaceContainer (or colorSurface)
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true
        )
        val colorSurface = typedValue.data

        binding.swipeRefresh.setColorSchemeColors(colorPrimary)
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(colorSurface)

        // Access the FAB from the Activity
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.isVisible = true
        fab.setOnClickListener {
            val bottomSheet = AddRecordBottomSheet { description ->
                // Now we can easily call the ViewModel!
                viewModel.startTimer(description)
            }
            bottomSheet.show(parentFragmentManager, AddRecordBottomSheet.TAG)
        }

        setupFilterChips()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recordsAdapter
        }
    }

    private fun observeState() {
        // Start a coroutine that repeats when the lifecycle is at least STARTED
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // ALWAYS turn off the refresh spinner when we get a result (Success OR Error)
                    if (state !is RecordsUiState.Loading) {
                        binding.swipeRefresh.isRefreshing = false
                    }

                    when (state) {
                        is RecordsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.recyclerView.isVisible = false
                            binding.errorText.isVisible = false
                        }

                        is RecordsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerView.isVisible = true
                            binding.errorText.isVisible = false
                            recordsAdapter.submitList(state.records)
                        }

                        is RecordsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.errorText.isVisible = true
                            binding.errorText.text = state.message

                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        true.also { requireActivity().findViewById<View>(R.id.topAppBar).isVisible = true }
    }

    private fun setupFilterChips() {
        binding.timeRangeGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val chipId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            when (chipId) {
                R.id.chipToday -> {
                    val (start, end) = DateRanges.getToday()
                    viewModel.loadRecords(start, end)
                }

                R.id.chipWeek -> {
                    val (start, end) = DateRanges.getLast7Days()
                    viewModel.loadRecords(start, end)
                }

                R.id.chipMonth -> {
                    val (start, end) = DateRanges.getThisMonth()
                    viewModel.loadRecords(start, end)
                }

                R.id.chipCustom -> {
                    showDateRangePicker()
                }
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Custom Range")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            // Selection is Pair<Long, Long> in Milliseconds UTC
            val startMs = selection.first
            val endMs = selection.second

            // Convert to Seconds
            viewModel.loadRecords(startMs / 1000, endMs / 1000)
        }

        picker.addOnNegativeButtonClickListener {
            // Reset to default chip if they cancel?
            binding.chipWeek.isChecked = true
        }

        picker.show(parentFragmentManager, "rangePicker")
    }
}