package com.example.shipmonitoring.ui.screens.nahkoda

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.ui.common.CenteredInlineLoading
import com.example.shipmonitoring.ui.common.SubmissionDetailDialog
import com.example.shipmonitoring.ui.common.SubmissionSummaryCard

private enum class NahkodaMenu(val label: String, val icon: ImageVector) {
    PENGAJUAN("Pengajuan", Icons.AutoMirrored.Filled.Assignment),
    HISTORY("History", Icons.Default.History),
    PROFIL("Profil", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NahkodaDashboardScreen(
    viewModel: NahkodaViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    val profile by viewModel.profile.collectAsState()
    val submissionState by viewModel.submissionState.collectAsState()
    val isTrackingLive by viewModel.isTrackingLive.collectAsState()

    val history by viewModel.history.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    val historyError by viewModel.historyError.collectAsState()

    var selectedMenu by rememberSaveable { mutableStateOf(NahkodaMenu.PENGAJUAN.name) }
    var selectedHistorySubmission by remember { mutableStateOf<SubmissionResponse?>(null) }

    var captainName by remember { mutableStateOf("") }
    var employeeCount by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var cargoAmount by remember { mutableStateOf("") }

    var sailingPermitUri by remember { mutableStateOf<Uri?>(null) }
    var callSignCertificateUri by remember { mutableStateOf<Uri?>(null) }
    var safetyCertificateUri by remember { mutableStateOf<Uri?>(null) }
    var radioStationPermitUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(profile.userName) {
        if (captainName.isBlank() && profile.userName.isNotBlank()) {
            captainName = profile.userName
        }
    }

    val sailingPermitPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        sailingPermitUri = it
    }
    val callSignPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        callSignCertificateUri = it
    }
    val safetyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        safetyCertificateUri = it
    }
    val radioPermitPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        radioStationPermitUri = it
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            viewModel.startLiveLocation(context)
            Toast.makeText(context, "Radar GPS aktif", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk live location.", Toast.LENGTH_LONG).show()
        }
    }

    val employeeValid = employeeCount.toIntOrNull()?.let { it > 0 } == true
    val isFormValid =
        isTrackingLive &&
            captainName.isNotBlank() &&
            employeeValid &&
            cargo.isNotBlank() &&
            cargoAmount.isNotBlank() &&
            sailingPermitUri != null &&
            callSignCertificateUri != null &&
            safetyCertificateUri != null &&
            radioStationPermitUri != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nahkoda", fontWeight = FontWeight.SemiBold) },
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
                NahkodaMenu.entries.forEach { menu ->
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
        when (NahkodaMenu.valueOf(selectedMenu)) {
            NahkodaMenu.PENGAJUAN -> NahkodaPengajuanTab(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                profile = profile,
                isTrackingLive = isTrackingLive,
                submissionState = submissionState,
                captainName = captainName,
                employeeCount = employeeCount,
                cargo = cargo,
                cargoAmount = cargoAmount,
                sailingPermitUri = sailingPermitUri,
                callSignCertificateUri = callSignCertificateUri,
                safetyCertificateUri = safetyCertificateUri,
                radioStationPermitUri = radioStationPermitUri,
                isFormValid = isFormValid,
                onCaptainNameChange = { captainName = it },
                onEmployeeCountChange = { employeeCount = it.filter { ch -> ch.isDigit() } },
                onCargoChange = { cargo = it },
                onCargoAmountChange = { cargoAmount = it },
                onPickSailingPermit = { sailingPermitPicker.launch("application/pdf") },
                onPickCallSignCertificate = { callSignPicker.launch("application/pdf") },
                onPickSafetyCertificate = { safetyPicker.launch("application/pdf") },
                onPickRadioStationPermit = { radioPermitPicker.launch("application/pdf") },
                onToggleTracking = { checked ->
                    if (checked) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        viewModel.stopLiveLocation()
                    }
                },
                onSubmit = {
                    viewModel.submitBerthingRequest(
                        context = context,
                        captainName = captainName,
                        employeeCount = employeeCount,
                        cargo = cargo,
                        cargoAmount = cargoAmount,
                        sailingPermitUri = sailingPermitUri!!,
                        callSignCertificateUri = callSignCertificateUri!!,
                        safetyCertificateUri = safetyCertificateUri!!,
                        radioStationPermitUri = radioStationPermitUri!!
                    )
                },
                onResetForm = {
                    viewModel.resetSubmissionState()
                    employeeCount = ""
                    cargo = ""
                    cargoAmount = ""
                    sailingPermitUri = null
                    callSignCertificateUri = null
                    safetyCertificateUri = null
                    radioStationPermitUri = null
                }
            )

            NahkodaMenu.HISTORY -> NahkodaHistoryTab(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                history = history,
                isHistoryLoading = isHistoryLoading,
                historyError = historyError,
                onRefresh = { viewModel.loadMySubmissionHistory() },
                onDetail = { selectedHistorySubmission = it }
            )

            NahkodaMenu.PROFIL -> NahkodaProfilTab(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                profile = profile,
                isTrackingLive = isTrackingLive
            )
        }
    }

    selectedHistorySubmission?.let { submission ->
        SubmissionDetailDialog(
            submission = submission,
            onDismiss = { selectedHistorySubmission = null }
        )
    }
}

@Composable
private fun NahkodaPengajuanTab(
    modifier: Modifier,
    profile: NahkodaProfile,
    isTrackingLive: Boolean,
    submissionState: SubmissionState,
    captainName: String,
    employeeCount: String,
    cargo: String,
    cargoAmount: String,
    sailingPermitUri: Uri?,
    callSignCertificateUri: Uri?,
    safetyCertificateUri: Uri?,
    radioStationPermitUri: Uri?,
    isFormValid: Boolean,
    onCaptainNameChange: (String) -> Unit,
    onEmployeeCountChange: (String) -> Unit,
    onCargoChange: (String) -> Unit,
    onCargoAmountChange: (String) -> Unit,
    onPickSailingPermit: () -> Unit,
    onPickCallSignCertificate: () -> Unit,
    onPickSafetyCertificate: () -> Unit,
    onPickRadioStationPermit: () -> Unit,
    onToggleTracking: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onResetForm: () -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTrackingLive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isTrackingLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Radar Aktif", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isTrackingLive) {
                                "Lokasi sedang dikirim ke sistem"
                            } else {
                                "Radar belum aktif"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isTrackingLive,
                        onCheckedChange = onToggleTracking
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Form Pengajuan Berlabuh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Nomor Kapal: ${profile.shipNumber ?: "-"}")
                    Text("Nama Kapal: ${profile.shipName ?: "-"}")

                    HorizontalDivider()

                    OutlinedTextField(
                        value = captainName,
                        onValueChange = onCaptainNameChange,
                        label = { Text("Nama Nahkoda") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = employeeCount,
                        onValueChange = onEmployeeCountChange,
                        label = { Text("Jumlah Pegawai") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = cargo,
                        onValueChange = onCargoChange,
                        label = { Text("Muatan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cargoAmount,
                        onValueChange = onCargoAmountChange,
                        label = { Text("Jumlah Muatan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    DocumentPickerField(
                        label = "Dokumen Surat Izin Berlayar",
                        selected = sailingPermitUri != null,
                        onPick = onPickSailingPermit
                    )
                    DocumentPickerField(
                        label = "Dokumen Surat Tanda Panggilan",
                        selected = callSignCertificateUri != null,
                        onPick = onPickCallSignCertificate
                    )
                    DocumentPickerField(
                        label = "Dokumen Sertifikat Keselamatan",
                        selected = safetyCertificateUri != null,
                        onPick = onPickSafetyCertificate
                    )
                    DocumentPickerField(
                        label = "Dokumen Surat Izin Stasiun Radio Kapal",
                        selected = radioStationPermitUri != null,
                        onPick = onPickRadioStationPermit
                    )

                    if (!isTrackingLive) {
                        Text(
                            text = "Aktifkan live location terlebih dahulu sebelum mengirim pengajuan.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    when (submissionState) {
                        SubmissionState.Loading -> {
                            CenteredInlineLoading()
                        }

                        is SubmissionState.Success -> {
                            Text(
                                text = submissionState.message,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Button(onClick = onResetForm) {
                                Text("Buat Pengajuan Baru")
                            }
                        }

                        is SubmissionState.Error -> {
                            Text(
                                text = submissionState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        SubmissionState.Idle -> Unit
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = isFormValid && submissionState !is SubmissionState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Kirim Pengajuan Berlabuh")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NahkodaHistoryTab(
    modifier: Modifier,
    history: List<SubmissionResponse>,
    isHistoryLoading: Boolean,
    historyError: String?,
    onRefresh: () -> Unit,
    onDetail: (SubmissionResponse) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Riwayat Pengajuan Kapal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh history")
                }
            }
        }

        if (isHistoryLoading) {
            item { CenteredInlineLoading() }
        }

        if (!historyError.isNullOrBlank()) {
            item {
                Text(
                    text = historyError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!isHistoryLoading && history.isEmpty()) {
            item {
                Text(
                    text = "Belum ada pengajuan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(history, key = { it.id }) { submission ->
            SubmissionSummaryCard(
                submission = submission,
                showValidationAction = false,
                onDetail = onDetail
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NahkodaProfilTab(
    modifier: Modifier,
    profile: NahkodaProfile,
    isTrackingLive: Boolean
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Profil Nahkoda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Nama: ${profile.userName.ifBlank { "-" }}")
                Text("Nomor Kapal: ${profile.shipNumber ?: "-"}")
                Text("Nama Kapal: ${profile.shipName ?: "-"}")
                Text(
                    text = if (isTrackingLive) {
                        "Status Radar GPS: Aktif"
                    } else {
                        "Status Radar GPS: Nonaktif"
                    },
                    color = if (isTrackingLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DocumentPickerField(
    label: String,
    selected: Boolean,
    onPick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (selected) "File PDF dipilih" else "Belum memilih file",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onPick) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(3.dp))
                Text("Pilih")
            }
        }
    }
}
