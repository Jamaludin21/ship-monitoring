package com.example.shipmonitoring.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipmonitoring.data.api.RetrofitClient
import com.example.shipmonitoring.data.model.LoginRequest
import com.example.shipmonitoring.data.model.UserData
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
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    val user = response.body()!!.data!!
                    // TODO: Simpan Token di DataStore / SharedPreferences di sini
                    _authState.value = AuthState.Success(user)
                } else {
                    _authState.value = AuthState.Error("Login Gagal: Periksa kredensial Anda")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Terjadi kesalahan jaringan: ${e.message}")
            }
        }
    }
}