package com.example.shipmonitoring.ui.screens.admin

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shipmonitoring.data.model.ShipLocation
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.ui.common.SubmissionDetailDialog
import com.example.shipmonitoring.ui.common.SubmissionSummaryCard
import com.example.shipmonitoring.ui.common.formatDateTime
import com.example.shipmonitoring.ui.common.isLocationActive
import com.example.shipmonitoring.ui.common.relativeTimeLabel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
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

    val actionState by viewModel.actionState.collectAsState()

    val filteredSubmissions = if (showPendingOnly) {
        submissions.filter { it.status.equals("PENDING", ignoreCase = true) }
    } else {
        submissions
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Admin", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout(onDone = onLogout)
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Validasi Pengajuan") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History Kapal") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Live Location") }
                )
            }

            when (selectedTab) {
                0 -> ValidationTab(
                    submissions = filteredSubmissions,
                    isLoading = isSubmissionsLoading,
                    errorMessage = submissionsError,
                    actionState = actionState,
                    showPendingOnly = showPendingOnly,
                    onTogglePending = { showPendingOnly = it },
                    onRefresh = { viewModel.refreshSubmissions() },
                    onDismissMessage = { viewModel.resetActionState() },
                    onDetail = { selectedSubmission = it },
                    onApprove = { approveTarget = it },
                    onReject = {
                        rejectTarget = it
                        rejectReason = ""
                    }
                )

                1 -> AdminHistoryTab(
                    query = shipNumberQuery,
                    onQueryChange = { shipNumberQuery = it },
                    history = shipHistory,
                    isLoading = isHistoryLoading,
                    errorMessage = historyError,
                    onSearch = { viewModel.searchShipHistory(shipNumberQuery) },
                    onDetail = { selectedSubmission = it }
                )

                else -> AdminLiveLocationTab(
                    locations = locations,
                    isRefreshing = isRefreshingLocation,
                    errorMessage = locationError,
                    onRefresh = { viewModel.refreshLocationOnce() }
                )
            }
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
                Button(onClick = {
                    viewModel.approveSubmission(submission.id)
                    approveTarget = null
                }) {
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
                Button(onClick = {
                    viewModel.rejectSubmission(submission.id, rejectReason)
                    if (rejectReason.isNotBlank()) {
                        rejectTarget = null
                    }
                }) {
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Validasi Pengajuan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = showPendingOnly, onCheckedChange = onTogglePending)
                        Text("Tampilkan hanya PENDING")
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh submissions")
                }
            }
        }

        when (actionState) {
            SubmissionActionState.Loading -> item { CircularProgressIndicator() }
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
            item { CircularProgressIndicator() }
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
    query: String,
    onQueryChange: (String) -> Unit,
    history: List<SubmissionResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    onSearch: () -> Unit,
    onDetail: (SubmissionResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("History Kapal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by nomor kapal") },
                singleLine = true
            )
        }

        item {
            Button(onClick = onSearch) {
                Text("Cari History")
            }
        }

        if (isLoading) {
            item { CircularProgressIndicator() }
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

        if (!isLoading && history.isEmpty()) {
            item { Text("Data history belum tersedia.") }
        }

        items(history, key = { it.id }) { submission ->
            SubmissionSummaryCard(
                submission = submission,
                showValidationAction = false,
                onDetail = onDetail
            )
        }
    }
}

@Composable
private fun AdminLiveLocationTab(
    locations: List<ShipLocation>,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    val startPosition = LatLng(-6.1021, 106.8833)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPosition, 11f)
    }

    var isMapLoaded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val activeCount = locations.count { isLocationActive(it.lastUpdatedAt) }
        Text("Kapal aktif: $activeCount/${locations.size}", fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live Location")
            IconButton(onClick = onRefresh) {
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
                    val activeLabel = if (isLocationActive(ship.lastUpdatedAt)) "Kapal aktif" else "Tidak aktif"
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
            items(locations, key = { it.shipId }) { ship ->
                AdminShipLocationInfoCard(ship)
            }
        }
    }
}

@Composable
private fun AdminShipLocationInfoCard(ship: ShipLocation) {
    val isActive = isLocationActive(ship.lastUpdatedAt)
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
                Text("$statusText (${relativeTimeLabel(ship.lastUpdatedAt)})", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
