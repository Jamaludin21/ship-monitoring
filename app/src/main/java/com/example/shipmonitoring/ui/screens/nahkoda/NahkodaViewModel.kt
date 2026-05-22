package com.example.shipmonitoring.ui.screens.nahkoda

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipmonitoring.data.api.ApiService
import com.example.shipmonitoring.data.api.AppContainer
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.data.model.UpdateLocationRequest
import com.example.shipmonitoring.data.session.SessionManager
import com.example.shipmonitoring.utils.LocationClient
import com.example.shipmonitoring.utils.extractErrorMessage
import com.example.shipmonitoring.utils.getFileFromUri
import com.example.shipmonitoring.utils.getFileSizeFromUri
import com.example.shipmonitoring.utils.toUserFriendlyNetworkMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class NahkodaProfile(
    val userName: String = "",
    val shipId: String? = null,
    val shipNumber: String? = null,
    val shipName: String? = null
)

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    data class Success(val message: String) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

class NahkodaViewModel : ViewModel() {
    private val apiService: ApiService = AppContainer.apiService
    private val sessionManager: SessionManager = AppContainer.sessionManager

    private val _profile = MutableStateFlow(NahkodaProfile())
    val profile: StateFlow<NahkodaProfile> = _profile.asStateFlow()

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    private val _history = MutableStateFlow<List<SubmissionResponse>>(emptyList())
    val history: StateFlow<List<SubmissionResponse>> = _history.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

    private var locationJob: Job? = null
    private val _isTrackingLive = MutableStateFlow(false)
    val isTrackingLive: StateFlow<Boolean> = _isTrackingLive.asStateFlow()

    init {
        observeSession()
        loadMySubmissionHistory()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.sessionFlow.collectLatest { session ->
                _profile.value = if (session == null) {
                    NahkodaProfile()
                } else {
                    NahkodaProfile(
                        userName = session.userName,
                        shipId = session.shipId,
                        shipNumber = session.shipNumber,
                        shipName = session.shipName
                    )
                }
            }
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = SubmissionState.Idle
    }

    fun startLiveLocation(context: Context) {
        val shipId = profile.value.shipId
        if (shipId.isNullOrBlank()) {
            _submissionState.value = SubmissionState.Error("Data kapal tidak ditemukan. Silakan login ulang.")
            return
        }

        if (_isTrackingLive.value) return

        val locationClient = LocationClient(context)
        _isTrackingLive.value = true

        locationJob = locationClient.getLocationUpdates(15000L)
            .catch {
                _isTrackingLive.value = false
                _submissionState.value = SubmissionState.Error("Gagal mengambil lokasi real-time.")
            }
            .onEach { location: Location ->
                sendLocation(shipId, location)
            }
            .launchIn(viewModelScope)
    }

    fun stopLiveLocation() {
        _isTrackingLive.value = false
        locationJob?.cancel()
        locationJob = null
    }

    private suspend fun sendLocation(shipId: String, location: Location) {
        try {
            val req = UpdateLocationRequest(shipId, location.latitude, location.longitude)
            apiService.updateLocation(req)
        } catch (_: Exception) {
            // Keep tracking alive even if one request fails.
        }
    }

    fun submitBerthingRequest(
        context: Context,
        captainName: String,
        employeeCount: String,
        cargo: String,
        cargoAmount: String,
        sailingPermitUri: Uri,
        callSignCertificateUri: Uri,
        safetyCertificateUri: Uri,
        radioStationPermitUri: Uri
    ) {
        viewModelScope.launch {
            val employeeCountValue = employeeCount.toIntOrNull()
            if (employeeCountValue == null || employeeCountValue <= 0) {
                _submissionState.value = SubmissionState.Error("Jumlah pegawai harus berupa angka lebih dari 0.")
                return@launch
            }

            _submissionState.value = SubmissionState.Loading
            val tempFiles = mutableListOf<File>()

            try {
                val captainNameBody = captainName.trim().toRequestBody("text/plain".toMediaType())
                val employeeCountBody = employeeCountValue.toString().toRequestBody("text/plain".toMediaType())
                val cargoBody = cargo.trim().toRequestBody("text/plain".toMediaType())
                val cargoAmountBody = cargoAmount.trim().toRequestBody("text/plain".toMediaType())

                val sailingPermitPart = buildPdfPart(context, "sailingPermit", sailingPermitUri, tempFiles)
                val callSignPart = buildPdfPart(context, "callSignCertificate", callSignCertificateUri, tempFiles)
                val safetyPart = buildPdfPart(context, "safetyCertificate", safetyCertificateUri, tempFiles)
                val radioPermitPart = buildPdfPart(context, "radioStationPermit", radioStationPermitUri, tempFiles)

                val response = apiService.createSubmission(
                    captainName = captainNameBody,
                    employeeCount = employeeCountBody,
                    cargo = cargoBody,
                    cargoAmount = cargoAmountBody,
                    sailingPermit = sailingPermitPart,
                    callSignCertificate = callSignPart,
                    safetyCertificate = safetyPart,
                    radioStationPermit = radioPermitPart
                )

                if (response.isSuccessful) {
                    _submissionState.value = SubmissionState.Success(
                        response.body()?.message ?: "Pengajuan berhasil dikirim."
                    )
                    loadMySubmissionHistory()
                } else {
                    _submissionState.value = SubmissionState.Error(
                        extractErrorMessage(response, "Gagal mengirim pengajuan")
                    )
                }
            } catch (e: IllegalArgumentException) {
                _submissionState.value = SubmissionState.Error(e.message ?: "Dokumen tidak valid.")
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error(
                    toUserFriendlyNetworkMessage(e)
                )
            } finally {
                tempFiles.forEach { file -> if (file.exists()) file.delete() }
            }
        }
    }

    private fun buildPdfPart(
        context: Context,
        partName: String,
        uri: Uri,
        cleanupBucket: MutableList<File>
    ): MultipartBody.Part {
        validatePdf(uri, context)

        val file = getFileFromUri(context, uri)
            ?: throw IllegalArgumentException("Gagal membaca dokumen PDF untuk $partName")

        cleanupBucket += file

        val requestFile = file.asRequestBody("application/pdf".toMediaType())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    private fun validatePdf(uri: Uri, context: Context) {
        val mimeType = context.contentResolver.getType(uri)?.lowercase()
        if (mimeType != "application/pdf") {
            throw IllegalArgumentException("Semua dokumen wajib berformat PDF.")
        }

        val size = getFileSizeFromUri(context, uri)
        val maxSize = 5L * 1024L * 1024L
        if (size != null && size > maxSize) {
            throw IllegalArgumentException("Ukuran file maksimal 5 MB untuk setiap dokumen.")
        }
    }

    fun loadMySubmissionHistory() {
        viewModelScope.launch {
            _isHistoryLoading.value = true
            _historyError.value = null

            try {
                val response = apiService.getMySubmissionHistory()
                if (response.isSuccessful) {
                    _history.value = response.body()?.data.orEmpty()
                } else {
                    _historyError.value = extractErrorMessage(response, "Gagal memuat riwayat pengajuan")
                }
            } catch (e: Exception) {
                _historyError.value = toUserFriendlyNetworkMessage(e)
            } finally {
                _isHistoryLoading.value = false
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            stopLiveLocation()
            sessionManager.clearSession()
            onDone()
        }
    }
}
