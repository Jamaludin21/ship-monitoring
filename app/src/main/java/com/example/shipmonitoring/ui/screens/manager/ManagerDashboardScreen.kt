package com.example.shipmonitoring.ui.screens.manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shipmonitoring.data.api.RetrofitClient
import com.example.shipmonitoring.data.model.ShipLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(onLogout: () -> Unit) {
    val startPosition = LatLng(-6.1021, 106.8833) // Pelabuhan Tanjung Priok
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPosition, 12f)
    }

    var shipsData by remember { mutableStateOf<List<ShipLocation>>(emptyList()) }

    // State baru untuk optimasi UI
    var isMapLoaded by remember { mutableStateOf(false) } // Deteksi peta sudah siap atau belum
    var isRefreshing by remember { mutableStateOf(false) } // Deteksi saat narik data dari API

    // Polling API
    LaunchedEffect(Unit) {
        while (true) {
            try {
                isRefreshing = true // Nyalakan indikator refresh

                // Gunakan Dispatchers.IO agar request jaringan benar-benar di background thread
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getAllShipLocations()
                }

                if (response.isSuccessful) {
                    shipsData = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshing = false // Matikan indikator refresh
            }
            delay(10000) // Tunggu 10 detik
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pusat Komando (Manager)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Menggunakan Box agar bisa menumpuk Peta dan Loading Indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Google Maps
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NORMAL),
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                onMapLoaded = {
                    isMapLoaded = true // Beri tahu UI bahwa Maps sudah selesai rendering
                }
            ) {
                shipsData.forEach { ship ->
                    val shipPosition = LatLng(ship.latitude, ship.longitude)
                    Marker(
                        state = MarkerState(position = shipPosition),
                        title = "Kapal: ${ship.shipName}",
                        snippet = "Latitude: ${ship.latitude}, Long: ${ship.longitude}"
                    )
                }
            }

            // 2. Layar Loading Peta (Hanya muncul saat pertama kali buka layar)
            AnimatedVisibility(
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Memuat Peta Pelabuhan...", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // 3. Indikator Sync/Refresh Data (Muncul di pojok atas Peta tiap 10 detik)
            AnimatedVisibility(
                visible = isRefreshing && isMapLoaded, // Hanya muncul jika peta sudah siap
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sinkronisasi radar...", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}