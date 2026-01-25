package net.ardevd.tagius.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ardevd.tagius.R
import net.ardevd.tagius.core.network.LoginRetrofitClient
import net.ardevd.tagius.core.ui.UIText
import retrofit2.HttpException
import java.io.IOException

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: UIText, val field: ErrorField) : LoginUiState
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
                    emitError(UIText.StringResource(R.string.error_invalid_token), ErrorField.TOKEN)
                } else if (e.code() == 404) {
                    emitError(UIText.StringResource(R.string.error_api_not_found), ErrorField.URL)
                } else {
                    emitError(
                        UIText.StringResource(R.string.error_server_generic, e.code()),
                        ErrorField.GENERAL
                    )
                }
            } catch (e: IOException) {
                // Network Errors (DNS, Connection Refused, Timeout)
                emitError(UIText.StringResource(R.string.error_connection_failed), ErrorField.URL)
            } catch (e: Exception) {
                emitError(
                    UIText.DynamicString(e.localizedMessage ?: "Unknown error"),
                    ErrorField.GENERAL
                )
            }
        }
    }

    private fun emitError(message: UIText, field: ErrorField) {
        _uiState.value = LoginUiState.Error(message, field)
    }
}