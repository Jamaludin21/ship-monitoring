package com.example.shipmonitoring.ui.screens.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shipmonitoring.data.model.ShipLocation
import com.example.shipmonitoring.data.model.ShipSummaryResponse
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.ui.common.CenteredInlineLoading
import com.example.shipmonitoring.ui.common.rememberCurrentTimeMillis
import com.example.shipmonitoring.ui.common.SubmissionDetailDialog
import com.example.shipmonitoring.ui.common.SubmissionSummaryCard
import com.example.shipmonitoring.ui.common.formatDateTime
import com.example.shipmonitoring.ui.common.isLocationActive
import com.example.shipmonitoring.ui.common.relativeTimeLabel
import com.example.shipmonitoring.ui.common.statusLabel
import com.example.shipmonitoring.utils.getDisplayNameFromUri
import com.example.shipmonitoring.utils.sanitizeShipNumberInput
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

private enum class AdminMenu(val label: String, val icon: ImageVector) {
    VALIDASI("Validasi", Icons.Default.AssignmentTurnedIn),
    HISTORY("History", Icons.Default.History),
    LOKASI("Lokasi", Icons.Default.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedMenu by rememberSaveable { mutableStateOf(AdminMenu.VALIDASI.name) }
    var selectedSubmission by remember { mutableStateOf<SubmissionResponse?>(null) }
    var shipNumberQuery by remember { mutableStateOf("") }
    var showPendingOnly by remember { mutableStateOf(true) }

    var approveTarget by remember { mutableStateOf<SubmissionResponse?>(null) }
    var rejectTarget by remember { mutableStateOf<SubmissionResponse?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    val submissions by viewModel.submissions.collectAsState()
    val isSubmissionsLoading by viewModel.isSubmissionsLoading.collectAsState()
    val submissionsError by viewModel.submissionsError.collectAsState()

    val shipHistory by viewModel.shipHistory.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    val historyError by viewModel.historyError.collectAsState()

    val locations by viewModel.locations.collectAsState()
    val isRefreshingLocation by viewModel.isRefreshingLocation.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val ships by viewModel.ships.collectAsState()
    val isShipsLoading by viewModel.isShipsLoading.collectAsState()
    val shipsError by viewModel.shipsError.collectAsState()

    val actionState by viewModel.actionState.collectAsState()
    val inspectionDataBySubmission by viewModel.inspectionDataBySubmission.collectAsState()

    val filteredSubmissions = if (showPendingOnly) {
        submissions.filter { it.status.equals("PENDING", ignoreCase = true) }
    } else {
        submissions
    }

    val reviewedSubmissions = remember(submissions) {
        submissions.filterNot { it.status.equals("PENDING", ignoreCase = true) }
    }
    val isHistoryFilteredByShip = shipNumberQuery.isNotBlank()
    val historyItems = if (isHistoryFilteredByShip) shipHistory else reviewedSubmissions
    val historyLoadingState = if (isHistoryFilteredByShip) isHistoryLoading else isSubmissionsLoading
    val historyErrorState = if (isHistoryFilteredByShip) historyError else submissionsError

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin", fontWeight = FontWeight.SemiBold) },
                expandedHeight = 52.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout(onDone = onLogout)
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AdminMenu.entries.forEach { menu ->
                    NavigationBarItem(
                        selected = selectedMenu == menu.name,
                        onClick = { selectedMenu = menu.name },
                        icon = { Icon(menu.icon, contentDescription = menu.label) },
                        label = { Text(menu.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (AdminMenu.valueOf(selectedMenu)) {
            AdminMenu.VALIDASI -> ValidationTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    submissions = filteredSubmissions,
                    isLoading = isSubmissionsLoading,
                    errorMessage = submissionsError,
                    actionState = actionState,
                    showPendingOnly = showPendingOnly,
                    onTogglePending = { showPendingOnly = it },
                    onRefresh = { viewModel.refreshSubmissions() },
                    onDismissMessage = { viewModel.resetActionState() },
                    onDetail = { submission ->
                        viewModel.openSubmissionDetail(submission) { latest ->
                            selectedSubmission = latest
                        }
                    },
                    onApprove = { approveTarget = it },
                    onReject = {
                        rejectTarget = it
                        rejectReason = ""
                    }
                )

            AdminMenu.HISTORY -> AdminHistoryTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    query = shipNumberQuery,
                    onQueryChange = { input ->
                        val sanitized = sanitizeShipNumberInput(input)
                        shipNumberQuery = sanitized
                        if (sanitized.isBlank()) {
                            viewModel.clearHistorySearchState()
                        }
                    },
                    history = historyItems,
                    inspectionDataBySubmission = inspectionDataBySubmission,
                    isLoading = historyLoadingState,
                    errorMessage = historyErrorState,
                    isFilteredByShip = isHistoryFilteredByShip,
                    onSearch = {
                        if (shipNumberQuery.isBlank()) {
                            viewModel.clearHistorySearchState()
                            viewModel.refreshSubmissions()
                        } else {
                            viewModel.searchShipHistory(shipNumberQuery)
                        }
                    },
                    onRefresh = {
                        if (shipNumberQuery.isBlank()) {
                            viewModel.refreshSubmissions()
                        } else {
                            viewModel.searchShipHistory(shipNumberQuery)
                        }
                    },
                    onDetail = { submission ->
                        viewModel.openSubmissionDetail(submission) { latest ->
                            selectedSubmission = latest
                        }
                    },
                    onChecklistModeChange = { submissionId, enabled ->
                        viewModel.setChecklistMode(submissionId, enabled)
                    },
                    onChecklistChoiceChange = { submissionId, itemNumber, choice ->
                        viewModel.updateChecklistChoice(submissionId, itemNumber, choice)
                    },
                    onChecklistNoteChange = { submissionId, itemNumber, note ->
                        viewModel.updateChecklistNote(submissionId, itemNumber, note)
                    },
                    onSummaryNoteChange = { submissionId, note ->
                        viewModel.updateSummaryNote(submissionId, note)
                    },
                    onInspectionDocChange = { submissionId, fileName, uriString ->
                        viewModel.updateInspectionDocument(submissionId, fileName, uriString)
                    },
                    onReplyDocChange = { submissionId, fileName, uriString ->
                        viewModel.updateReplyLetterDocument(submissionId, fileName, uriString)
                    },
                    onSaveInspection = { submissionId ->
                        viewModel.saveInspectionData(submissionId, context)
                    }
                )

            AdminMenu.LOKASI -> AdminLiveLocationTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    locations = locations,
                    ships = ships,
                    isRefreshing = isRefreshingLocation,
                    isShipsLoading = isShipsLoading,
                    errorMessage = locationError,
                    shipsError = shipsError,
                    onRefresh = {
                        viewModel.refreshLocationOnce()
                        viewModel.refreshShips()
                    }
                )
        }
    }

    selectedSubmission?.let { submission ->
        SubmissionDetailDialog(
            submission = submission,
            onDismiss = { selectedSubmission = null }
        )
    }

    approveTarget?.let { submission ->
        AlertDialog(
            onDismissRequest = { approveTarget = null },
            title = { Text("Konfirmasi") },
            text = { Text("Apakah Anda yakin ingin menyetujui pengajuan ini?") },
            confirmButton = {
                Button(
                    enabled = actionState !is SubmissionActionState.Loading,
                    onClick = {
                        val approvedShipNumber = submission.ship?.shipNumber.orEmpty()
                        viewModel.approveSubmission(submission.id) {
                            if (approvedShipNumber.isNotBlank()) {
                                selectedMenu = AdminMenu.HISTORY.name
                                shipNumberQuery = sanitizeShipNumberInput(approvedShipNumber)
                                viewModel.searchShipHistory(approvedShipNumber)
                            }
                        }
                        approveTarget = null
                    }
                ) {
                    Text("Setujui")
                }
            },
            dismissButton = {
                TextButton(onClick = { approveTarget = null }) {
                    Text("Batal")
                }
            }
        )
    }

    rejectTarget?.let { submission ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Alasan penolakan") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Alasan penolakan") }
                )
            },
            confirmButton = {
                Button(
                    enabled = actionState !is SubmissionActionState.Loading,
                    onClick = {
                        viewModel.rejectSubmission(submission.id, rejectReason)
                        if (rejectReason.isNotBlank()) {
                            rejectTarget = null
                        }
                    }
                ) {
                    Text("Tolak Pengajuan")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) {
                    Text("Batalkan")
                }
            }
        )
    }
}

@Composable
private fun ValidationTab(
    modifier: Modifier,
    submissions: List<SubmissionResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    actionState: SubmissionActionState,
    showPendingOnly: Boolean,
    onTogglePending: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDismissMessage: () -> Unit,
    onDetail: (SubmissionResponse) -> Unit,
    onApprove: (SubmissionResponse) -> Unit,
    onReject: (SubmissionResponse) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Validasi Pengajuan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (showPendingOnly) "Mode: hanya PENDING" else "Mode: semua pengajuan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pending", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(checked = showPendingOnly, onCheckedChange = onTogglePending)
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isLoading && actionState !is SubmissionActionState.Loading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh submissions")
                        }
                    }
                }
            }
        }

        when (actionState) {
            SubmissionActionState.Loading -> item { CenteredInlineLoading() }
            is SubmissionActionState.Success -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(actionState.message, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            TextButton(onClick = onDismissMessage) { Text("Tutup") }
                        }
                    }
                }
            }
            is SubmissionActionState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(actionState.message, color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = onDismissMessage) { Text("Tutup") }
                        }
                    }
                }
            }
            SubmissionActionState.Idle -> Unit
        }

        if (isLoading) {
            item { CenteredInlineLoading() }
        }

        if (!errorMessage.isNullOrBlank()) {
            item {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!isLoading && submissions.isEmpty()) {
            item { Text("Data pengajuan tidak ditemukan.") }
        }

        items(submissions, key = { it.id }) { submission ->
            SubmissionSummaryCard(
                submission = submission,
                showValidationAction = true,
                onDetail = onDetail,
                onApprove = onApprove,
                onReject = onReject
            )
        }
    }
}

@Composable
private fun AdminHistoryTab(
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    history: List<SubmissionResponse>,
    inspectionDataBySubmission: Map<String, SubmissionInspectionData>,
    isLoading: Boolean,
    errorMessage: String?,
    isFilteredByShip: Boolean,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDetail: (SubmissionResponse) -> Unit,
    onChecklistModeChange: (submissionId: String, enabled: Boolean) -> Unit,
    onChecklistChoiceChange: (submissionId: String, itemNumber: Int, choice: ChecklistChoice) -> Unit,
    onChecklistNoteChange: (submissionId: String, itemNumber: Int, note: String) -> Unit,
    onSummaryNoteChange: (submissionId: String, note: String) -> Unit,
    onInspectionDocChange: (submissionId: String, fileName: String?, uriString: String?) -> Unit,
    onReplyDocChange: (submissionId: String, fileName: String?, uriString: String?) -> Unit,
    onSaveInspection: (submissionId: String) -> Unit
) {
    val orderedHistory = remember(history) { history.sortedByDescending { it.submittedAt } }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("History Kapal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh history")
                }
            }
        }

        item {
            Text(
                text = if (isFilteredByShip) {
                    "Menampilkan history untuk kapal KM-$query."
                } else {
                    "Menampilkan semua pengajuan yang sudah diproses (APPROVED/REJECTED)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Nomor kapal") },
                    prefix = { Text("KM-") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Button(
                    onClick = onSearch,
                    enabled = !isLoading,
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cari")
                }
            }
        }

        if (isLoading) {
            item { CenteredInlineLoading() }
        }

        if (!errorMessage.isNullOrBlank()) {
            item {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!isLoading && orderedHistory.isEmpty()) {
            item {
                Text(
                    text = if (isFilteredByShip) {
                        "History kapal tidak ditemukan untuk nomor tersebut."
                    } else {
                        "Belum ada pengajuan yang sudah diproses."
                    }
                )
            }
        }

        items(orderedHistory, key = { it.id }) { submission ->
            val inspectionData = inspectionDataBySubmission[submission.id] ?: SubmissionInspectionData()

            SubmissionSummaryCard(
                submission = submission,
                showValidationAction = false,
                onDetail = onDetail
            )

            if (submission.status.equals("APPROVED", ignoreCase = true)) {
                AdminInspectionFormCard(
                    submissionId = submission.id,
                    inspectionData = inspectionData,
                    onChecklistModeChange = onChecklistModeChange,
                    onChecklistChoiceChange = onChecklistChoiceChange,
                    onChecklistNoteChange = onChecklistNoteChange,
                    onSummaryNoteChange = onSummaryNoteChange,
                    onInspectionDocChange = onInspectionDocChange,
                    onReplyDocChange = onReplyDocChange,
                    onSaveInspection = onSaveInspection
                )
            }
        }
    }
}

@Composable
private fun AdminInspectionFormCard(
    submissionId: String,
    inspectionData: SubmissionInspectionData,
    onChecklistModeChange: (submissionId: String, enabled: Boolean) -> Unit,
    onChecklistChoiceChange: (submissionId: String, itemNumber: Int, choice: ChecklistChoice) -> Unit,
    onChecklistNoteChange: (submissionId: String, itemNumber: Int, note: String) -> Unit,
    onSummaryNoteChange: (submissionId: String, note: String) -> Unit,
    onInspectionDocChange: (submissionId: String, fileName: String?, uriString: String?) -> Unit,
    onReplyDocChange: (submissionId: String, fileName: String?, uriString: String?) -> Unit,
    onSaveInspection: (submissionId: String) -> Unit
) {
    val context = LocalContext.current

    val inspectionDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val fileName = uri?.let { getDisplayNameFromUri(context, it) ?: it.lastPathSegment }
        onInspectionDocChange(submissionId, fileName, uri?.toString())
    }
    val replyDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val fileName = uri?.let { getDisplayNameFromUri(context, it) ?: it.lastPathSegment }
        onReplyDocChange(submissionId, fileName, uri?.toString())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Hasil Cek Kapal", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gunakan form checklist")
                Switch(
                    checked = inspectionData.useChecklistForm,
                    onCheckedChange = { checked -> onChecklistModeChange(submissionId, checked) }
                )
            }

            if (inspectionData.useChecklistForm) {
                inspectionData.checklistItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${item.number}. ${item.question}", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = item.choice == ChecklistChoice.YES,
                                    onClick = {
                                        onChecklistChoiceChange(submissionId, item.number, ChecklistChoice.YES)
                                    }
                                )
                                Text("Ya")
                                Spacer(modifier = Modifier.width(12.dp))
                                RadioButton(
                                    selected = item.choice == ChecklistChoice.NO,
                                    onClick = {
                                        onChecklistChoiceChange(submissionId, item.number, ChecklistChoice.NO)
                                    }
                                )
                                Text("Tidak")
                            }
                            OutlinedTextField(
                                value = item.note,
                                onValueChange = { note ->
                                    onChecklistNoteChange(submissionId, item.number, note)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Keterangan") },
                                singleLine = true
                            )
                        }
                    }
                }
            }

            PickedDocumentField(
                label = "Dokumen hasil cek",
                fileName = inspectionData.inspectionDocName,
                uriString = inspectionData.inspectionDocUri,
                onPick = { inspectionDocLauncher.launch("application/pdf") }
            )
            PickedDocumentField(
                label = "Surat balasan",
                fileName = inspectionData.replyLetterDocName,
                uriString = inspectionData.replyLetterDocUri,
                onPick = { replyDocLauncher.launch("application/pdf") }
            )

            OutlinedTextField(
                value = inspectionData.summaryNote,
                onValueChange = { note -> onSummaryNoteChange(submissionId, note) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Catatan hasil cek") }
            )

            Button(
                onClick = { onSaveInspection(submissionId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Hasil Cek")
            }

            inspectionData.updatedAtMillis?.let { updatedAt ->
                Text(
                    text = "Terakhir disimpan: ${formatMillisDateTime(updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PickedDocumentField(
    label: String,
    fileName: String?,
    uriString: String?,
    onPick: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val context = LocalContext.current
    val hasFile = !fileName.isNullOrBlank() && !uriString.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium)
                Text(
                    text = fileName ?: "Belum memilih file",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasFile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onPick) {
                Text("Pilih PDF")
            }
            OutlinedButton(
                enabled = hasFile,
                onClick = {
                    val opened = runCatching {
                        uriString?.let { uriHandler.openUri(it) }
                    }.isSuccess

                    if (!opened) {
                        Toast.makeText(
                            context,
                            "Gagal membuka dokumen. Coba muat ulang data.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            ) {
                Text("Buka")
            }
        }
    }
}

@Composable
private fun AdminLiveLocationTab(
    modifier: Modifier,
    locations: List<ShipLocation>,
    ships: List<ShipSummaryResponse>,
    isRefreshing: Boolean,
    isShipsLoading: Boolean,
    errorMessage: String?,
    shipsError: String?,
    onRefresh: () -> Unit
) {
    val nowMillis = rememberCurrentTimeMillis()
    val startPosition = LatLng(-6.1021, 106.8833)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPosition, 11f)
    }

    var isMapLoaded by remember { mutableStateOf(false) }
    var hasMapLoadTimedOut by remember { mutableStateOf(false) }
    var mapLoadAttempt by remember { mutableStateOf(0) }

    val requestRefresh: () -> Unit = {
        hasMapLoadTimedOut = false
        mapLoadAttempt += 1
        onRefresh()
    }

    LaunchedEffect(isMapLoaded, mapLoadAttempt) {
        if (isMapLoaded) {
            hasMapLoadTimedOut = false
            return@LaunchedEffect
        }

        hasMapLoadTimedOut = false
        delay(12_000L)
        if (!isMapLoaded) {
            hasMapLoadTimedOut = true
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val activeCount = locations.count { isLocationActive(it.lastUpdatedAt, nowMillis = nowMillis) }
        Text("Kapal aktif: $activeCount/${locations.size}", fontWeight = FontWeight.SemiBold)
        Text(
            text = "Aktif dihitung dari update lokasi <= 5 menit terakhir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live Location")
            IconButton(
                onClick = requestRefresh,
                enabled = !isRefreshing && !isShipsLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh lokasi")
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!shipsError.isNullOrBlank()) {
            Text(
                text = shipsError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NORMAL),
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                onMapLoaded = { isMapLoaded = true }
            ) {
                locations.forEach { ship ->
                    val shipPosition = LatLng(ship.latitude, ship.longitude)
                    val activeLabel = if (isLocationActive(ship.lastUpdatedAt, nowMillis = nowMillis)) "Kapal aktif" else "Tidak aktif"
                    Marker(
                        state = MarkerState(position = shipPosition),
                        title = "${ship.shipNumber} - ${ship.shipName}",
                        snippet = "Lat: ${ship.latitude}, Lng: ${ship.longitude}\nTerakhir update: ${formatDateTime(ship.lastUpdatedAt)}\nStatus: $activeLabel"
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !isMapLoaded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasMapLoadTimedOut) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Peta belum berhasil dimuat.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Untuk build release, pastikan API key Maps mengizinkan package com.example.shipmonitoring + SHA-1 release.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(onClick = requestRefresh) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isRefreshing && isMapLoaded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sinkronisasi lokasi...")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Semua Kapal (${ships.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isShipsLoading) {
                item { CenteredInlineLoading() }
            }

            if (!isShipsLoading && ships.isEmpty()) {
                item {
                    Text(
                        text = "Data kapal tidak ditemukan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(ships, key = { it.id }) { ship ->
                AdminShipSummaryCard(ship = ship)
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lokasi Kapal Aktif (${locations.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(locations, key = { it.shipId }) { ship ->
                AdminShipLocationInfoCard(ship = ship, nowMillis = nowMillis)
            }
        }
    }
}

@Composable
private fun AdminShipSummaryCard(ship: ShipSummaryResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("${ship.shipNumber} - ${ship.name}", fontWeight = FontWeight.SemiBold)
            Text("Nahkoda: ${ship.captain?.name ?: "-"}")
            Text("Lokasi terakhir: ${formatDateTime(ship.latestLocation?.createdAt)}")
            val latestStatus = ship.latestSubmission?.status
            Text(
                text = "Pengajuan terakhir: ${if (latestStatus.isNullOrBlank()) "-" else statusLabel(latestStatus)}"
            )
        }
    }
}

@Composable
private fun AdminShipLocationInfoCard(ship: ShipLocation, nowMillis: Long) {
    val isActive = isLocationActive(ship.lastUpdatedAt, nowMillis = nowMillis)
    val statusText = if (isActive) "Kapal aktif" else "Status: Tidak Aktif"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("${ship.shipNumber} - ${ship.shipName}", fontWeight = FontWeight.SemiBold)
            Text("Latitude: ${ship.latitude}")
            Text("Longitude: ${ship.longitude}")
            Text("Terakhir update: ${formatDateTime(ship.lastUpdatedAt)}")
            if (isActive) {
                Text(statusText, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("$statusText (${relativeTimeLabel(ship.lastUpdatedAt, nowMillis)})", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatMillisDateTime(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.forLanguageTag("id-ID"))
    return formatter.format(java.util.Date(millis))
}
