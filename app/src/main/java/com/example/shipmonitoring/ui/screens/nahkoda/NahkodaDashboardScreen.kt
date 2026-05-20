package com.example.shipmonitoring.ui.screens.nahkoda

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
    // Di aplikasi nyata, shipId didapat dari data login user
    shipId: String = "dummy-ship-id-123"
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()

    // Menyimpan URI file yang dipilih pengguna
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("Belum ada dokumen yang dipilih") }

    // Launcher untuk membuka File Manager (Hanya menerima PDF)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = if (uri != null) "Dokumen dipilih, siap diunggah" else "Batal memilih dokumen"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Nahkoda", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
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

            // Kartu Informasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Status Kapal: Menunggu Pengajuan", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Silakan unggah dokumen legalitas dan muatan kapal sebelum bersandar di pelabuhan.")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol Pilih File
            OutlinedButton(
                onClick = { documentPickerLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Pilih File")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Dokumen PDF")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indikator Nama File
            Text(text = selectedFileName, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(32.dp))

            // Reaktivitas UI Berdasarkan State Upload
            when (uploadState) {
                is UploadState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Mengunggah dokumen ke server...")
                }
                is UploadState.Success -> {
                    val message = (uploadState as UploadState.Success).message
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Sukses", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.resetState()
                            selectedFileUri = null
                            selectedFileName = "Belum ada dokumen yang dipilih"
                        }) {
                            Text("Ajukan Dokumen Lain")
                        }
                    }
                }
                is UploadState.Error -> {
                    val message = (uploadState as UploadState.Error).message
                    Text(message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Coba Lagi")
                    }
                }
                is UploadState.Idle -> {
                    // Tombol Unggah hanya aktif jika ada file yang dipilih
                    Button(
                        onClick = {
                            selectedFileUri?.let { uri ->
                                viewModel.uploadDokumen(context, uri, shipId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = selectedFileUri != null // Validasi cegah tekan saat kosong
                    ) {
                        Text("Kirim Dokumen Pengajuan", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}