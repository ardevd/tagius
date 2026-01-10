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
                // Use the dynamic range
                val result = repository.fetchRecords(start, end)
                if (result.isEmpty()) {
                    // TODO: might want a specific Empty state, but Success with empty list works too
                    _uiState.value = RecordsUiState.Success(emptyList())
                } else {
                    _uiState.value = RecordsUiState.Success(result)
                }
            } catch (e: Exception) {
                _uiState.value = RecordsUiState.Error(e.localizedMessage ?: "Error")
            }
        }
    }
}

class RecordsViewModelFactory(private val repository: RecordsRepository, private val tokenManager: TokenManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordsViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}