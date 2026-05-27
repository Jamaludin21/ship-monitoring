package com.example.shipmonitoring.ui.screens.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipmonitoring.data.api.ApiService
import com.example.shipmonitoring.data.api.AppContainer
import com.example.shipmonitoring.data.model.ChecklistQuestionResponse
import com.example.shipmonitoring.data.model.InspectionItemPayload
import com.example.shipmonitoring.data.model.RejectSubmissionRequest
import com.example.shipmonitoring.data.model.ShipLocation
import com.example.shipmonitoring.data.model.ShipSummaryResponse
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.data.session.SessionManager
import com.example.shipmonitoring.utils.extractErrorMessage
import com.example.shipmonitoring.utils.getFileFromUri
import com.example.shipmonitoring.utils.getFileSizeFromUri
import com.example.shipmonitoring.utils.isPdfUri
import com.example.shipmonitoring.utils.toUserFriendlyNetworkMessage
import com.example.shipmonitoring.utils.withKmPrefix
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

sealed class SubmissionActionState {
    object Idle : SubmissionActionState()
    object Loading : SubmissionActionState()
    data class Success(val message: String) : SubmissionActionState()
    data class Error(val message: String) : SubmissionActionState()
}

class AdminViewModel : ViewModel() {
    private val apiService: ApiService = AppContainer.apiService
    private val sessionManager: SessionManager = AppContainer.sessionManager
    private val gson = Gson()

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

    private val _actionState = MutableStateFlow<SubmissionActionState>(SubmissionActionState.Idle)
    val actionState: StateFlow<SubmissionActionState> = _actionState.asStateFlow()

    private val _arrivalChecklist = MutableStateFlow<List<ChecklistQuestionResponse>>(emptyList())
    val arrivalChecklist: StateFlow<List<ChecklistQuestionResponse>> = _arrivalChecklist.asStateFlow()

    private val _inspectionDataBySubmission = MutableStateFlow<Map<String, SubmissionInspectionData>>(emptyMap())
    val inspectionDataBySubmission: StateFlow<Map<String, SubmissionInspectionData>> = _inspectionDataBySubmission.asStateFlow()

    private var pollingJob: Job? = null

    init {
        refreshSubmissions()
        refreshShips()
        refreshArrivalInspectionChecklist()
        startLocationPolling()
    }

    fun resetActionState() {
        _actionState.value = SubmissionActionState.Idle
    }

    fun refreshSubmissions() {
        if (_isSubmissionsLoading.value) return

        viewModelScope.launch {
            _isSubmissionsLoading.value = true
            _submissionsError.value = null

            try {
                val response = apiService.getSubmissions()
                if (response.isSuccessful) {
                    val submissionList = response.body()?.data.orEmpty()
                    _submissions.value = submissionList
                    prefillInspectionData(submissionList)
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

    fun approveSubmission(id: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _actionState.value = SubmissionActionState.Loading

            try {
                val response = apiService.approveSubmission(id)
                if (response.isSuccessful) {
                    _actionState.value = SubmissionActionState.Success(
                        response.body()?.message ?: "Pengajuan berhasil disetujui"
                    )
                    refreshSubmissions()
                    onSuccess?.invoke()
                } else {
                    _actionState.value = SubmissionActionState.Error(
                        extractErrorMessage(response, "Gagal menyetujui pengajuan")
                    )
                }
            } catch (e: Exception) {
                _actionState.value = SubmissionActionState.Error(
                    toUserFriendlyNetworkMessage(e)
                )
            }
        }
    }

    fun upsertInspectionData(submissionId: String, updater: (SubmissionInspectionData) -> SubmissionInspectionData) {
        _inspectionDataBySubmission.update { oldMap ->
            val current = oldMap[submissionId] ?: buildInitialInspectionData()
            oldMap + (submissionId to updater(current))
        }
    }

    fun setChecklistMode(submissionId: String, enabled: Boolean) {
        upsertInspectionData(submissionId) { data ->
            data.copy(useChecklistForm = enabled)
        }
    }

    fun updateChecklistChoice(submissionId: String, itemNumber: Int, choice: ChecklistChoice) {
        upsertInspectionData(submissionId) { data ->
            data.copy(
                checklistItems = data.checklistItems.map { item ->
                    if (item.number == itemNumber) item.copy(choice = choice) else item
                }
            )
        }
    }

    fun updateChecklistNote(submissionId: String, itemNumber: Int, note: String) {
        upsertInspectionData(submissionId) { data ->
            data.copy(
                checklistItems = data.checklistItems.map { item ->
                    if (item.number == itemNumber) item.copy(note = note) else item
                }
            )
        }
    }

    fun updateSummaryNote(submissionId: String, note: String) {
        upsertInspectionData(submissionId) { data ->
            data.copy(summaryNote = note)
        }
    }

    fun updateInspectionDocument(submissionId: String, fileName: String?, uriString: String?) {
        upsertInspectionData(submissionId) { data ->
            data.copy(
                inspectionDocName = fileName,
                inspectionDocUri = uriString
            )
        }
    }

    fun updateReplyLetterDocument(submissionId: String, fileName: String?, uriString: String?) {
        upsertInspectionData(submissionId) { data ->
            data.copy(
                replyLetterDocName = fileName,
                replyLetterDocUri = uriString
            )
        }
    }

    fun saveInspectionData(submissionId: String, context: Context) {
        val localData = _inspectionDataBySubmission.value[submissionId] ?: buildInitialInspectionData()

        if (localData.useChecklistForm && localData.checklistItems.any { it.choice == ChecklistChoice.UNSET }) {
            _actionState.value = SubmissionActionState.Error("Semua item checklist wajib dipilih YA/TIDAK.")
            return
        }

        viewModelScope.launch {
            _actionState.value = SubmissionActionState.Loading

            val tempFiles = mutableListOf<File>()
            try {
                val inspectionItemsBody = if (localData.useChecklistForm) {
                    val payload = localData.checklistItems.map { item ->
                        InspectionItemPayload(
                            itemNo = item.number,
                            condition = when (item.choice) {
                                ChecklistChoice.YES -> "YES"
                                ChecklistChoice.NO -> "NO"
                                ChecklistChoice.UNSET -> "NO"
                            },
                            note = item.note.trim().ifBlank { null }
                        )
                    }

                    gson.toJson(payload).toRequestBody("application/json".toMediaType())
                } else {
                    null
                }

                val noteBody = localData.summaryNote
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.toRequestBody("text/plain".toMediaType())

                val inspectionDocPart = buildOptionalPdfPart(
                    context = context,
                    partName = "inspectionDocument",
                    uriString = localData.inspectionDocUri,
                    cleanupBucket = tempFiles
                )

                val replyLetterPart = buildOptionalPdfPart(
                    context = context,
                    partName = "responseLetter",
                    uriString = localData.replyLetterDocUri,
                    cleanupBucket = tempFiles
                )

                if (inspectionItemsBody == null && noteBody == null && inspectionDocPart == null && replyLetterPart == null) {
                    _actionState.value = SubmissionActionState.Error(
                        "Isi checklist, unggah dokumen hasil cek, unggah surat balasan, atau isi catatan."
                    )
                    return@launch
                }

                val response = apiService.upsertArrivalInspection(
                    id = submissionId,
                    inspectionItems = inspectionItemsBody,
                    note = noteBody,
                    inspectionDocument = inspectionDocPart,
                    responseLetter = replyLetterPart
                )

                if (response.isSuccessful) {
                    upsertInspectionData(submissionId) { data ->
                        data.copy(updatedAtMillis = System.currentTimeMillis())
                    }
                    _actionState.value = SubmissionActionState.Success(
                        response.body()?.message ?: "Hasil cek kapal berhasil disimpan."
                    )
                    refreshSubmissions()
                } else {
                    _actionState.value = SubmissionActionState.Error(
                        extractErrorMessage(response, "Gagal menyimpan hasil cek kapal")
                    )
                }
            } catch (e: IllegalArgumentException) {
                _actionState.value = SubmissionActionState.Error(
                    e.message ?: "Dokumen hasil cek tidak valid."
                )
            } catch (e: Exception) {
                _actionState.value = SubmissionActionState.Error(
                    toUserFriendlyNetworkMessage(e)
                )
            } finally {
                tempFiles.forEach { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }

    fun refreshArrivalInspectionChecklist() {
        viewModelScope.launch {
            try {
                val response = apiService.getArrivalInspectionChecklist()
                if (!response.isSuccessful) {
                    return@launch
                }

                val checklist = response.body()?.data.orEmpty()
                if (checklist.isEmpty()) {
                    return@launch
                }

                _arrivalChecklist.value = checklist
                syncChecklistWithServerItems(checklist)
            } catch (_: Exception) {
                // Use local fallback checklist if endpoint cannot be reached.
            }
        }
    }

    private fun buildInitialInspectionData(): SubmissionInspectionData {
        val serverChecklistItems = _arrivalChecklist.value
            .sortedBy { it.itemNo }
            .map { question ->
                InspectionChecklistItem(
                    number = question.itemNo,
                    question = question.question
                )
            }

        return SubmissionInspectionData(
            checklistItems = if (serverChecklistItems.isEmpty()) {
                defaultInspectionChecklistItems()
            } else {
                serverChecklistItems
            }
        )
    }

    private fun prefillInspectionData(submissions: List<SubmissionResponse>) {
        _inspectionDataBySubmission.update { oldMap ->
            val mutable = oldMap.toMutableMap()
            val templateChecklist = buildInitialInspectionData().checklistItems

            submissions.forEach { submission ->
                if (!submission.status.equals("APPROVED", ignoreCase = true)) {
                    return@forEach
                }
                if (mutable.containsKey(submission.id)) {
                    return@forEach
                }

                val inspection = submission.arrivalInspection
                val checklistFromServer = templateChecklist.map { template ->
                    val serverItem = inspection?.items
                        ?.firstOrNull { it.itemNo == template.number }

                    template.copy(
                        choice = when (serverItem?.condition?.uppercase()) {
                            "YES" -> ChecklistChoice.YES
                            "NO" -> ChecklistChoice.NO
                            else -> ChecklistChoice.UNSET
                        },
                        note = serverItem?.note.orEmpty()
                    )
                }

                mutable[submission.id] = SubmissionInspectionData(
                    checklistItems = checklistFromServer,
                    inspectionDocName = inspection?.inspectionDocumentUrl?.substringAfterLast('/'),
                    inspectionDocUri = inspection?.inspectionDocumentUrl,
                    replyLetterDocName = inspection?.responseLetterUrl?.substringAfterLast('/'),
                    replyLetterDocUri = inspection?.responseLetterUrl,
                    summaryNote = inspection?.note.orEmpty()
                )
            }

            mutable
        }
    }

    private fun syncChecklistWithServerItems(questions: List<ChecklistQuestionResponse>) {
        val serverItems = questions
            .sortedBy { it.itemNo }
            .map { question ->
                InspectionChecklistItem(
                    number = question.itemNo,
                    question = question.question
                )
            }

        if (serverItems.isEmpty()) {
            return
        }

        _inspectionDataBySubmission.update { oldMap ->
            oldMap.mapValues { (_, currentData) ->
                val existingByItemNo = currentData.checklistItems.associateBy { it.number }
                currentData.copy(
                    checklistItems = serverItems.map { defaultItem ->
                        val existingItem = existingByItemNo[defaultItem.number]
                        if (existingItem == null) {
                            defaultItem
                        } else {
                            defaultItem.copy(
                                choice = existingItem.choice,
                                note = existingItem.note
                            )
                        }
                    }
                )
            }
        }
    }

    private fun buildOptionalPdfPart(
        context: Context,
        partName: String,
        uriString: String?,
        cleanupBucket: MutableList<File>
    ): MultipartBody.Part? {
        if (uriString.isNullOrBlank()) {
            return null
        }

        val uri = Uri.parse(uriString)
        if (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) {
            return null
        }
        validatePdfUri(context, uri)
        val file = getFileFromUri(context, uri)
            ?: throw IllegalArgumentException("Gagal membaca file untuk $partName")

        cleanupBucket += file
        val requestFile = file.asRequestBody("application/pdf".toMediaType())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    private fun validatePdfUri(context: Context, uri: Uri) {
        if (!isPdfUri(context, uri)) {
            throw IllegalArgumentException("File harus berformat PDF.")
        }

        val sizeBytes = getFileSizeFromUri(context, uri)
        val maxBytes = 4L * 1024L * 1024L
        if (sizeBytes != null && sizeBytes > maxBytes) {
            throw IllegalArgumentException("Ukuran file maksimal 4MB.")
        }
    }

    fun rejectSubmission(id: String, reviewNote: String) {
        val note = reviewNote.trim()
        if (note.isBlank()) {
            _actionState.value = SubmissionActionState.Error("Alasan penolakan wajib diisi.")
            return
        }

        viewModelScope.launch {
            _actionState.value = SubmissionActionState.Loading

            try {
                val response = apiService.rejectSubmission(id, RejectSubmissionRequest(note))
                if (response.isSuccessful) {
                    _actionState.value = SubmissionActionState.Success(
                        response.body()?.message ?: "Pengajuan berhasil ditolak"
                    )
                    refreshSubmissions()
                } else {
                    _actionState.value = SubmissionActionState.Error(
                        extractErrorMessage(response, "Gagal menolak pengajuan")
                    )
                }
            } catch (e: Exception) {
                _actionState.value = SubmissionActionState.Error(
                    toUserFriendlyNetworkMessage(e)
                )
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
