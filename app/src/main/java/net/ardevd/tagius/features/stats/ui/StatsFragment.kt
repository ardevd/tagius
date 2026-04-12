package net.ardevd.tagius.features.stats.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.patrykandpatrick.vico.views.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.views.cartesian.data.columnSeries
import com.patrykandpatrick.vico.views.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.views.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.views.common.data.ExtraStore
import kotlinx.coroutines.launch
import net.ardevd.tagius.R
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.core.network.RetrofitClient
import net.ardevd.tagius.core.utils.DateRanges
import net.ardevd.tagius.databinding.FragmentStatsBinding
import net.ardevd.tagius.features.records.data.RecordsRepository
import net.ardevd.tagius.features.records.viewmodel.RecordsUiState
import net.ardevd.tagius.features.records.viewmodel.RecordsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val modelProducer = CartesianChartModelProducer()

    private val bottomAxisLabelKey = ExtraStore.Key<List<String>>()

    private val bottomAxisValueFormatter = CartesianValueFormatter { context, x, _ ->
        val labels = context.model.extraStore[bottomAxisLabelKey]
        val index = x.toInt()
        if (index >= 0 && index < labels.size) labels[index] else ""
    }

    private val viewModel: RecordsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = requireContext().applicationContext
                val tokenManager = TokenManager(context)
                val apiService = RetrofitClient.getInstance(context)
                val repository = RecordsRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                return RecordsViewModel(repository, tokenManager) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatsBinding.bind(view)

        binding.columnChart.modelProducer = modelProducer
        with(binding.columnChart) {
            chart = chart?.copy(
                bottomAxis = (chart?.bottomAxis as? HorizontalAxis)?.copy(
                    valueFormatter = bottomAxisValueFormatter
                )
            )
        }

        setupFilterChips()
        observeState()
        setupMenu()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RecordsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.contentLayout.isVisible = false
                            binding.errorText.isVisible = false
                        }
                        is RecordsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.contentLayout.isVisible = true
                            binding.errorText.isVisible = false
                            updateCharts(state.records)
                        }
                        is RecordsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.contentLayout.isVisible = false
                            binding.errorText.isVisible = true
                            binding.errorText.text = state.message.asString(requireContext())
                        }
                    }
                }
            }
        }
    }

    private fun updateCharts(records: List<net.ardevd.tagius.core.data.TimeTaggerRecord>) {
        // Tag Pie Chart
        val tagsMap = mutableMapOf<String, Long>()
        val dayDurationMap = mutableMapOf<Long, Long>()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        for (record in records) {
            val duration = record.endTime - record.startTime
            if (duration <= 0) continue

            // Tags breakdown
            val tags = net.ardevd.tagius.core.ui.tagRegexPattern.toRegex().findAll(record.description).map { it.value.lowercase() }.toList()
            if (tags.isEmpty()) {
                tagsMap["No Tag"] = tagsMap.getOrDefault("No Tag", 0L) + duration
            } else {
                for (tag in tags) {
                    tagsMap[tag] = tagsMap.getOrDefault(tag, 0L) + duration
                }
            }

            // Day duration breakdown
            val dayString = sdf.format(Date(record.startTime * 1000))
            val startOfDay = sdf.parse(dayString)?.time?.div(1000) ?: 0L
            dayDurationMap[startOfDay] = dayDurationMap.getOrDefault(startOfDay, 0L) + duration
        }

        // Setup Pie Chart
        binding.pieChart.setData(tagsMap)

        // Setup Column Chart
        val sortedDays = dayDurationMap.keys.sorted()
        
        val filledDays = mutableListOf<Long>()
        if (sortedDays.isNotEmpty()) {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeZone = TimeZone.getDefault()
            calendar.timeInMillis = sortedDays.first() * 1000
            val endMillis = sortedDays.last() * 1000
            while (calendar.timeInMillis <= endMillis) {
                filledDays.add(calendar.timeInMillis / 1000)
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }

        var durationsList = filledDays.map { (dayDurationMap[it] ?: 0L) / 3600f }

        val sdfDay = SimpleDateFormat("d", Locale.getDefault())
        var labelsList = filledDays.map { sdfDay.format(Date(it * 1000)) }

        if (durationsList.isEmpty()) {
            durationsList = listOf(0f)
            labelsList = listOf("-")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            modelProducer.runTransaction {
                columnSeries {
                    series(durationsList)
                }
                extras { it[bottomAxisLabelKey] = labelsList }
            }
        }
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
                    // Custom date logic here (reuse material picker if desired, or skip for now)
                    val (start, end) = DateRanges.getLast7Days()
                    viewModel.loadRecords(start, end)
                    binding.chipWeek.isChecked = true
                }
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == android.R.id.home) {
                    parentFragmentManager.popBackStack()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.title_statistics)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
