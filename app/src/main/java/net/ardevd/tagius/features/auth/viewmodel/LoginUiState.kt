package net.ardevd.tagius.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ardevd.tagius.core.network.LoginRetrofitClient
import retrofit2.HttpException
import java.io.IOException

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: String, val field: ErrorField) : LoginUiState
}

enum class ErrorField { URL, TOKEN, GENERAL }

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun verifyAndLogin(url: String, token: String) {
        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {
                // Create a temporary service with these exact credentials
                val tempService = LoginRetrofitClient.createTemporaryService(url, token)

                // Try to hit the API
                tempService.getSettings()

                // If no exception thrown, we are good!
                _uiState.value = LoginUiState.Success

            } catch (e: HttpException) {
                // HTTP Errors (Server reached, but rejected us)
                if (e.code() == 401 || e.code() == 403) {
                    _uiState.value = LoginUiState.Error("Invalid API Token", ErrorField.TOKEN)
                } else if (e.code() == 404) {
                    _uiState.value = LoginUiState.Error("API not found at this URL", ErrorField.URL)
                } else {
                    _uiState.value = LoginUiState.Error("Server error: ${e.code()}", ErrorField.GENERAL)
                }
            } catch (e: IOException) {
                // Network Errors (DNS, Connection Refused, Timeout)
                _uiState.value = LoginUiState.Error("Could not connect to server. ${e.message}", ErrorField.URL)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Unknown error", ErrorField.GENERAL)
            }
        }
    }
}