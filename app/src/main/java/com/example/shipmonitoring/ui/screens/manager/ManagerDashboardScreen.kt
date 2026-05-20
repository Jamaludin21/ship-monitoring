package com.example.shipmonitoring.ui.screens.manager

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.shipmonitoring.data.api.RetrofitClient
import com.example.shipmonitoring.data.model.ShipLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen() {
    // Titik awal peta (Misal: Pelabuhan Tanjung Priok)
    val startPosition = LatLng(-6.1021, 106.8833)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPosition, 12f)
    }

    // State untuk menampung data kapal dari backend
    var shipsData by remember { mutableStateOf<List<ShipLocation>>(emptyList()) }

    // Efek untuk memanggil API secara berkala (Polling setiap 10 detik)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = RetrofitClient.apiService.getAllShipLocations()
                if (response.isSuccessful) {
                    shipsData = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(10000) // Tunggu 10 detik sebelum me-refresh posisi kapal lagi
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitoring Peta (Manager)") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        // Menampilkan Peta Google
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.NORMAL)
        ) {
            // Looping data kapal dan membuat Marker (Pin) untuk masing-masing
            shipsData.forEach { ship ->
                val shipPosition = LatLng(ship.latitude, ship.longitude)
                Marker(
                    state = MarkerState(position = shipPosition),
                    title = "Kapal: ${ship.shipName}",
                    snippet = "Posisi Terakhir Diperbarui",
                    // Anda bisa mengganti ikon pin dengan ikon kapal custom jika punya asetnya
                )
            }
        }
    }
}