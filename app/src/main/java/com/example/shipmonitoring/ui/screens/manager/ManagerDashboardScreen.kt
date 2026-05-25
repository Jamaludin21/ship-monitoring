package com.example.shipmonitoring.ui.screens.manager

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shipmonitoring.data.model.ShipLocation
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.ui.common.CenteredInlineLoading
import com.example.shipmonitoring.ui.common.rememberCurrentTimeMillis
import com.example.shipmonitoring.ui.common.SubmissionDetailDialog
import com.example.shipmonitoring.ui.common.SubmissionSummaryCard
import com.example.shipmonitoring.ui.common.formatDateTime
import com.example.shipmonitoring.ui.common.isLocationActive
import com.example.shipmonitoring.ui.common.relativeTimeLabel
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

private enum class ManagerMenu(val label: String, val icon: ImageVector) {
    PENGAJUAN("Pengajuan", Icons.Default.Inbox),
    HISTORY("History", Icons.Default.History),
    LOKASI("Lokasi", Icons.Default.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    onLogout: () -> Unit,
    viewModel: ManagerViewModel = viewModel()
) {
    var selectedMenu by rememberSaveable { mutableStateOf(ManagerMenu.PENGAJUAN.name) }
    var selectedSubmission by remember { mutableStateOf<SubmissionResponse?>(null) }
    var shipNumberQuery by remember { mutableStateOf("") }

    val submissions by viewModel.submissions.collectAsState()
    val isSubmissionsLoading by viewModel.isSubmissionsLoading.collectAsState()
    val submissionsError by viewModel.submissionsError.collectAsState()

    val shipHistory by viewModel.shipHistory.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    val historyError by viewModel.historyError.collectAsState()

    val locations by viewModel.locations.collectAsState()
    val isRefreshingLocation by viewModel.isRefreshingLocation.collectAsState()
    val locationError by viewModel.locationError.collectAsState()

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
                title = { Text("Manager", fontWeight = FontWeight.SemiBold) },
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
                ManagerMenu.entries.forEach { menu ->
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
        when (ManagerMenu.valueOf(selectedMenu)) {
            ManagerMenu.PENGAJUAN -> SubmissionInboxTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    submissions = submissions,
                    isLoading = isSubmissionsLoading,
                    errorMessage = submissionsError,
                    onRefresh = { viewModel.refreshSubmissions() },
                    onDetail = { selectedSubmission = it }
                )

            ManagerMenu.HISTORY -> SubmissionHistoryTab(
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
                    onDetail = { selectedSubmission = it }
                )

            ManagerMenu.LOKASI -> LiveLocationTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    locations = locations,
                    isRefreshing = isRefreshingLocation,
                    errorMessage = locationError,
                    onRefresh = { viewModel.refreshLocationOnce() }
                )
        }
    }

    selectedSubmission?.let { submission ->
        SubmissionDetailDialog(
            submission = submission,
            onDismiss = { selectedSubmission = null }
        )
    }
}

@Composable
private fun SubmissionInboxTab(
    modifier: Modifier,
    submissions: List<SubmissionResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onDetail: (SubmissionResponse) -> Unit
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
                    Text("Pengajuan Masuk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh submissions")
                    }
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

        if (!isLoading && submissions.isEmpty()) {
            item { Text("Belum ada data pengajuan.") }
        }

        items(submissions, key = { it.id }) { submission ->
            SubmissionSummaryCard(
                submission = submission,
                showValidationAction = false,
                onDetail = onDetail
            )
        }
    }
}

@Composable
private fun SubmissionHistoryTab(
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    history: List<SubmissionResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    isFilteredByShip: Boolean,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDetail: (SubmissionResponse) -> Unit
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
                IconButton(onClick = onRefresh) {
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
            SubmissionSummaryCard(
                submission = submission,
                showValidationAction = false,
                onDetail = onDetail
            )
        }
    }
}

@Composable
private fun LiveLocationTab(
    modifier: Modifier,
    locations: List<ShipLocation>,
    isRefreshing: Boolean,
    errorMessage: String?,
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
            IconButton(onClick = requestRefresh) {
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
                                androidx.compose.material3.OutlinedButton(onClick = requestRefresh) {
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
            items(locations, key = { it.shipId }) { ship ->
                ShipLocationInfoCard(ship = ship, nowMillis = nowMillis)
            }
        }
    }
}

@Composable
private fun ShipLocationInfoCard(ship: ShipLocation, nowMillis: Long) {
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
