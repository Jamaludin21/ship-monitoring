package com.example.shipmonitoring.ui.screens.nahkoda

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NahkodaDashboardScreen(
    viewModel: NahkodaViewModel,
    shipId: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()

    // Memantau state apakah GPS sedang menyala atau mati dari ViewModel
    val isTrackingLive by viewModel.isTrackingLive.collectAsState()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("Belum ada dokumen yang dipilih") }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = if (uri != null) "Dokumen dipilih, siap diunggah" else "Batal memilih dokumen"
    }

    // --- REQUEST PERMISSION UNTUK LOKASI (BEST PRACTICE) ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isFineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val isCoarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (isFineLocationGranted || isCoarseLocationGranted) {
            // Izin Diberikan -> Nyalakan GPS!
            viewModel.startLiveLocation(context, shipId)
            Toast.makeText(context, "Radar GPS Aktif", Toast.LENGTH_SHORT).show()
        } else {
            // Izin Ditolak
            Toast.makeText(context, "Izin Lokasi Diperlukan untuk melacak kapal", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Nahkoda", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =====================================
            // BAGIAN 1: KONTROL LOKASI KAPAL (BARU)
            // =====================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTrackingLive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isTrackingLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sistem Radar (Live Location)", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isTrackingLive) "Menyiarkan posisi kapal..." else "Radar dimatikan",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isTrackingLive,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Minta Izin dulu sebelum menyalakan
                                locationPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            } else {
                                viewModel.stopLiveLocation()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            // =====================================
            // BAGIAN 2: UPLOAD DOKUMEN KAPAL
            // =====================================
            Text("Pengajuan Dokumen Berlabuh", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { documentPickerLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Pilih File")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Dokumen PDF")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = selectedFileName, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(24.dp))

            when (uploadState) {
                is UploadState.Loading -> CircularProgressIndicator()
                is UploadState.Success -> {
                    val message = (uploadState as UploadState.Success).message
                    Icon(Icons.Default.CheckCircle, contentDescription = "Sukses", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Button(onClick = {
                        viewModel.resetState()
                        selectedFileUri = null
                        selectedFileName = "Belum ada dokumen yang dipilih"
                    }) { Text("Ajukan Lainnya") }
                }
                is UploadState.Error -> {
                    val message = (uploadState as UploadState.Error).message
                    Text(message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.resetState() }) { Text("Coba Lagi") }
                }
                is UploadState.Idle -> {
                    Button(
                        onClick = {
                            selectedFileUri?.let { uri -> viewModel.uploadDokumen(context, uri, shipId) }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = selectedFileUri != null
                    ) {
                        Text("Kirim Dokumen Pengajuan", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}