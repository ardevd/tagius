package net.ardevd.tagius.features.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.core.utils.DateRanges
import net.ardevd.tagius.features.records.data.RecordsRepository
import java.util.regex.Pattern

class RecordsViewModel(
    private val repository: RecordsRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    // Backing property to avoid state updates from outside
    private val _uiState = MutableStateFlow<RecordsUiState>(RecordsUiState.Loading)

    // The UI collects from this StateFlow
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    // Cache the full list
    private var allRecords: List<TimeTaggerRecord> = emptyList()

    // Keep track of the current query to re-apply it if data refreshes in background
    private var currentQuery: String = ""

    private var currentStart: Long = 0
    private var currentEnd: Long = 0

    init {
        // Default to last 7 days
        val (start, end) = DateRanges.getLast7Days()
        loadRecords(start, end)
    }

    val lastDescription: StateFlow<String> = tokenManager.lastDescriptionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    fun startTimer(description: String) {
        viewModelScope.launch {
            val success = repository.startRecord(description)

            if (success) {
                // Store description
                tokenManager.saveLastDescription(description)
                // Refresh the list to show the new running record
                loadRecords()
            } else {
                _uiState.value = RecordsUiState.Error("Failed to start timer")
            }
        }
    }

    fun stopRecord(record: TimeTaggerRecord) {
        viewModelScope.launch {
            val success = repository.stopRecord(record)

            if (success) {
                // If successful, reload the list to show the new state
                loadRecords()
            } else {
                _uiState.value = RecordsUiState.Error("Failed to stop record")
            }
        }
    }

    fun updateRecord(
        record: TimeTaggerRecord,
        newDescription: String,
        newStart: Long,
        newEnd: Long
    ) {
        viewModelScope.launch {
            val success = repository.updateRecord(record, newDescription, newStart, newEnd)
            if (success) loadRecords() else _uiState.value = RecordsUiState.Error("Update failed")
        }
    }

    fun deleteRecord(record: TimeTaggerRecord) {
        viewModelScope.launch {
            val success = repository.deleteRecord(record)
            if (success) loadRecords() else _uiState.value = RecordsUiState.Error("Delete failed")
        }
    }


    fun loadRecords(start: Long = currentStart, end: Long = currentEnd) {
        currentStart = start
        currentEnd = end

        viewModelScope.launch {
            _uiState.value = RecordsUiState.Loading
            try {
                // Fetch data
                val result = repository.fetchRecords(start, end)
                // Cache it
                allRecords = result
                // Apply any existing filter (or show all)
                applyFilter()

            } catch (e: Exception) {
                _uiState.value = RecordsUiState.Error(e.localizedMessage ?: "Error")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        if (currentQuery.isBlank()) {
            _uiState.value = RecordsUiState.Success(allRecords)
        } else {
            // Case-insensitive filter on the description
            val filtered = allRecords.filter { record ->
                record.description.contains(currentQuery, ignoreCase = true)
            }
            _uiState.value = RecordsUiState.Success(filtered)
        }
    }

    fun getTopTags(limit: Int = 10): List<String> {
        // Regex to find tags (starts with #, followed by letters/numbers/dashes/underscores)
        val tagPattern = Regex("#[\\w\\-]+")

        // Flatten all descriptions into a list of tags
        val allTags = allRecords.flatMap { record ->
            // Lowercase the tags.
            tagPattern.findAll(record.description).map { it.value.lowercase() }
        }

        // Group by tag, count frequency, sort descending, and take top N
        return allTags
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { (_, count) -> count }
            .map { (tag, _) -> tag }
            .take(limit)
    }
}

