package com.example.shipmonitoring.ui.screens.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipmonitoring.data.api.ApiService
import com.example.shipmonitoring.data.api.AppContainer
import com.example.shipmonitoring.data.model.ShipLocation
import com.example.shipmonitoring.data.model.ShipSummaryResponse
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.data.session.SessionManager
import com.example.shipmonitoring.utils.extractErrorMessage
import com.example.shipmonitoring.utils.toUserFriendlyNetworkMessage
import com.example.shipmonitoring.utils.withKmPrefix
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ManagerViewModel : ViewModel() {
    private val apiService: ApiService = AppContainer.apiService
    private val sessionManager: SessionManager = AppContainer.sessionManager

    private val _submissions = MutableStateFlow<List<SubmissionResponse>>(emptyList())
    val submissions: StateFlow<List<SubmissionResponse>> = _submissions.asStateFlow()

    private val _isSubmissionsLoading = MutableStateFlow(false)
    val isSubmissionsLoading: StateFlow<Boolean> = _isSubmissionsLoading.asStateFlow()

    private val _submissionsError = MutableStateFlow<String?>(null)
    val submissionsError: StateFlow<String?> = _submissionsError.asStateFlow()

    private val _shipHistory = MutableStateFlow<List<SubmissionResponse>>(emptyList())
    val shipHistory: StateFlow<List<SubmissionResponse>> = _shipHistory.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

    private val _locations = MutableStateFlow<List<ShipLocation>>(emptyList())
    val locations: StateFlow<List<ShipLocation>> = _locations.asStateFlow()

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation: StateFlow<Boolean> = _isRefreshingLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _ships = MutableStateFlow<List<ShipSummaryResponse>>(emptyList())
    val ships: StateFlow<List<ShipSummaryResponse>> = _ships.asStateFlow()

    private val _isShipsLoading = MutableStateFlow(false)
    val isShipsLoading: StateFlow<Boolean> = _isShipsLoading.asStateFlow()

    private val _shipsError = MutableStateFlow<String?>(null)
    val shipsError: StateFlow<String?> = _shipsError.asStateFlow()

    private var pollingJob: Job? = null

    init {
        refreshSubmissions()
        refreshShips()
        startLocationPolling()
    }

    fun refreshSubmissions() {
        if (_isSubmissionsLoading.value) return

        viewModelScope.launch {
            _isSubmissionsLoading.value = true
            _submissionsError.value = null

            try {
                val response = apiService.getSubmissions()
                if (response.isSuccessful) {
                    _submissions.value = response.body()?.data.orEmpty()
                } else {
                    _submissionsError.value = extractErrorMessage(response, "Gagal memuat data pengajuan")
                }
            } catch (e: Exception) {
                _submissionsError.value = toUserFriendlyNetworkMessage(e)
            } finally {
                _isSubmissionsLoading.value = false
            }
        }
    }

    fun searchShipHistory(shipNumber: String) {
        if (_isHistoryLoading.value) return

        val normalizedShipNumber = withKmPrefix(shipNumber)
        if (normalizedShipNumber.isBlank()) {
            _shipHistory.value = emptyList()
            _historyError.value = "Nomor kapal wajib diisi."
            return
        }

        viewModelScope.launch {
            _isHistoryLoading.value = true
            _historyError.value = null

            try {
                val response = apiService.getShipHistory(normalizedShipNumber)
                if (response.isSuccessful) {
                    _shipHistory.value = response.body()?.data.orEmpty()
                } else {
                    if (response.code() == 409) {
                        _historyError.value = "Nomor kapal terlalu umum. Gunakan nomor kapal lengkap, contoh: KM-001."
                        return@launch
                    }
                    _historyError.value = extractErrorMessage(response, "Gagal memuat history kapal")
                }
            } catch (e: Exception) {
                _historyError.value = toUserFriendlyNetworkMessage(e)
            } finally {
                _isHistoryLoading.value = false
            }
        }
    }

    fun clearHistorySearchState() {
        _shipHistory.value = emptyList()
        _historyError.value = null
        _isHistoryLoading.value = false
    }

    fun refreshLocationOnce() {
        if (_isRefreshingLocation.value) return

        viewModelScope.launch {
            _isRefreshingLocation.value = true
            _locationError.value = null

            try {
                val response = apiService.getAllShipLocations()
                if (response.isSuccessful) {
                    _locations.value = response.body()?.data.orEmpty()
                } else {
                    _locationError.value = extractErrorMessage(response, "Gagal memuat live location")
                }
            } catch (e: Exception) {
                _locationError.value = toUserFriendlyNetworkMessage(e)
            } finally {
                _isRefreshingLocation.value = false
            }
        }
    }

    fun refreshShips() {
        if (_isShipsLoading.value) return

        viewModelScope.launch {
            _isShipsLoading.value = true
            _shipsError.value = null

            try {
                val response = apiService.getShips()
                if (response.isSuccessful) {
                    _ships.value = response.body()?.data.orEmpty()
                } else {
                    _shipsError.value = extractErrorMessage(response, "Gagal memuat data kapal")
                }
            } catch (e: Exception) {
                _shipsError.value = toUserFriendlyNetworkMessage(e)
            } finally {
                _isShipsLoading.value = false
            }
        }
    }

    private fun startLocationPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                refreshLocationOnce()
                delay(10_000L)
            }
        }
    }

    fun openSubmissionDetail(
        fallback: SubmissionResponse,
        onReady: (SubmissionResponse) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.getSubmissionDetail(fallback.id)
                val detailed = response.body()?.data
                if (response.isSuccessful && detailed != null) {
                    onReady(detailed)
                } else {
                    onReady(fallback)
                }
            } catch (_: Exception) {
                onReady(fallback)
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
