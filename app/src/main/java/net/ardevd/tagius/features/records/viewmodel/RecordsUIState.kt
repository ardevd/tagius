package net.ardevd.tagius.features.records.viewmodel

import net.ardevd.tagius.core.data.TimeTaggerRecord

sealed interface RecordsUiState {
    object Loading : RecordsUiState
    data class Success(val records: List<TimeTaggerRecord>) : RecordsUiState
    data class Error(val message: String) : RecordsUiState
}