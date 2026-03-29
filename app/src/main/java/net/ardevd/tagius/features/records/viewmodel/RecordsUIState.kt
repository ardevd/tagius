package net.ardevd.tagius.features.records.viewmodel

import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.ui.UIText

sealed interface RecordsUiState {
    object Loading : RecordsUiState
    data class Success(val records: List<TimeTaggerRecord>) : RecordsUiState
    data class Error(val message: UIText) : RecordsUiState
}