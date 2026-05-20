package com.example.shipmonitoring.ui.screens.nahkoda

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipmonitoring.data.api.RetrofitClient
import com.example.shipmonitoring.data.model.UpdateLocationRequest
import com.example.shipmonitoring.utils.LocationClient
import com.example.shipmonitoring.utils.getFileFromUri
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    data class Success(val message: String) : UploadState()
    data class Error(val message: String) : UploadState()
}

class NahkodaViewModel : ViewModel() {

    // --- STATE & FUNGSI UPLOAD DOKUMEN ---
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun uploadDokumen(context: Context, uri: Uri, shipId: String) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val file = getFileFromUri(context, uri)
                if (file == null) {
                    _uploadState.value = UploadState.Error("Gagal membaca file yang dipilih.")
                    return@launch
                }
                val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                val documentPart = MultipartBody.Part.createFormData("document", file.name, requestFile)
                val shipIdPart = shipId.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.apiService.uploadDocument(shipIdPart, documentPart)
                if (response.isSuccessful) {
                    _uploadState.value = UploadState.Success("Dokumen berhasil diajukan untuk bersandar.")
                    if (file.exists()) file.delete()
                } else {
                    _uploadState.value = UploadState.Error("Gagal mengunggah dokumen: ${response.code()}")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error("Terjadi kesalahan jaringan: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uploadState.value = UploadState.Idle
    }

    // --- STATE & FUNGSI LIVE LOCATION (Dipindah ke dalam class) ---
    private var locationJob: Job? = null
    val isTrackingLive = MutableStateFlow(false)

    fun startLiveLocation(context: Context, shipId: String) {
        val locationClient = LocationClient(context)
        isTrackingLive.value = true

        locationJob = locationClient.getLocationUpdates(15000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location: Location ->
                try {
                    // Menggunakan UpdateLocationRequest agar tidak bentrok
                    val req = UpdateLocationRequest(shipId, location.latitude, location.longitude)
                    RetrofitClient.apiService.updateLocation(req)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .launchIn(viewModelScope) // Sekarang viewModelScope bisa terbaca
    }

    fun stopLiveLocation() {
        isTrackingLive.value = false
        locationJob?.cancel()
    }
}