package net.ardevd.tagius.features.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.features.records.data.RecordsRepository

class RecordsViewModel(
    private val repository: RecordsRepository
) : ViewModel() {

    // Backing property to avoid state updates from outside
    private val _uiState = MutableStateFlow<RecordsUiState>(RecordsUiState.Loading)

    // The UI collects from this StateFlow
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init {
        // Automatically load records when the ViewModel is created
        loadRecords()
    }

    fun startTimer(description: String) {
        viewModelScope.launch {
            // Optional: Set a loading state or "optimistic update" here

            val success = repository.startRecord(description)

            if (success) {
                // Refresh the list to show the new running record
                loadRecords()
            } else {
                _uiState.value = RecordsUiState.Error("Failed to start timer")
            }
        }
    }

    fun stopRecord(record: TimeTaggerRecord) {
        viewModelScope.launch {
            // Optional: Could set a specific "Stopping..." UI state here if you wanted
            val success = repository.stopRecord(record)

            if (success) {
                // If successful, reload the list to show the new state
                loadRecords()
            } else {
                // Handle failure (e.g., show a Toast or Snackbar via a side-effect channel)
                _uiState.value = RecordsUiState.Error("Failed to stop record")
            }
        }
    }

    fun updateRecord(record: TimeTaggerRecord, newDescription: String, newStart: Long, newEnd: Long) {
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

    fun loadRecords() {
        viewModelScope.launch {
            _uiState.value = RecordsUiState.Loading
            try {
                // Fetch last 7 days
                // TODO: Make it user configurable
                val now = System.currentTimeMillis() / 1000
                val oneWeekAgo = now - (24 * 24 * 60 * 60)

                val result = repository.fetchRecords(oneWeekAgo, now)

                if (result.isEmpty()) {
                    // TODO: might want a specific Empty state, but Success with empty list works too
                    _uiState.value = RecordsUiState.Success(emptyList())
                } else {
                    _uiState.value = RecordsUiState.Success(result)
                }
            } catch (e: Exception) {
                _uiState.value = RecordsUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}

class RecordsViewModelFactory(private val repository: RecordsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}