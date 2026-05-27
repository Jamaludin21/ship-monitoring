package com.example.shipmonitoring.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipmonitoring.data.api.ApiService
import com.example.shipmonitoring.data.api.AppContainer
import com.example.shipmonitoring.data.model.LoginRequest
import com.example.shipmonitoring.data.model.UserData
import com.example.shipmonitoring.data.session.SessionManager
import com.example.shipmonitoring.utils.extractErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Sealed class untuk memanajemen state UI
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: UserData) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val apiService: ApiService = AppContainer.apiService
    private val sessionManager: SessionManager = AppContainer.sessionManager

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(username: String, password: String) {
        if (_authState.value is AuthState.Loading) return

        val normalizedUsername = username.trim()
        if (normalizedUsername.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Username dan password wajib diisi.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(LoginRequest(normalizedUsername, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    val body = response.body()!!
                    val user = body.data
                    val token = body.token
                    if (user == null || token.isNullOrBlank()) {
                        _authState.value = AuthState.Error("Data login tidak valid dari server.")
                        return@launch
                    }

                    sessionManager.saveSession(
                        token = token,
                        role = user.role,
                        name = user.name,
                        userId = user.id,
                        shipId = user.resolvedShipId,
                        shipNumber = user.resolvedShipNumber,
                        shipName = user.resolvedShipName
                    )

                    _authState.value = AuthState.Success(user)
                } else {
                    val message = extractErrorMessage(response, "Login gagal")
                    _authState.value = AuthState.Error(message)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Terjadi kesalahan jaringan: ${e.message}")
            }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
