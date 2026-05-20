package com.example.shipmonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shipmonitoring.ui.screens.auth.AuthViewModel
import com.example.shipmonitoring.ui.screens.auth.LoginScreen
import com.example.shipmonitoring.ui.screens.manager.ManagerDashboardScreen
import com.example.shipmonitoring.ui.screens.nahkoda.NahkodaDashboardScreen
import com.example.shipmonitoring.ui.screens.nahkoda.NahkodaViewModel
import com.example.shipmonitoring.ui.theme.ShipMonitoringTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShipMonitoringTheme {
                // Inisialisasi NavController untuk mengatur perpindahan layar
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // NavHost adalah "wadah" untuk semua layar aplikasi Anda
                    NavHost(
                        navController = navController,
                        startDestination = "login", // Layar pertama yang dibuka
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // 1. Rute Layar Login
                        composable("login") {
                            // Inisialisasi ViewModel untuk Login
                            val authViewModel: AuthViewModel = viewModel()

                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = { role ->
                                    // Logika perpindahan layar berdasarkan Role dari API
                                    when (role.uppercase()) {
                                        "NAHKODA" -> {
                                            navController.navigate("nahkoda_dashboard") {
                                                // Hapus layar login dari riwayat (Backstack)
                                                // Agar saat di-back, aplikasi keluar, bukan kembali ke login
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                        "MANAGER" -> {
                                            navController.navigate("manager_dashboard") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                        "ADMIN" -> {
                                            // TODO: Jika nanti ada AdminDashboardScreen, arahkan ke sini
                                        }
                                    }
                                }
                            )
                        }

                        // 2. Rute Layar Dashboard Nahkoda
                        composable("nahkoda_dashboard") {
                            // Inisialisasi ViewModel untuk Nahkoda
                            val nahkodaViewModel: NahkodaViewModel = viewModel()

                            NahkodaDashboardScreen(
                                viewModel = nahkodaViewModel,
                                // Di aplikasi nyata, shipId didapat dari data login (misal via SharedPreferences)
                                shipId = "dummy-ship-id-123"
                            )
                        }

                        // 3. Rute Layar Dashboard Manager
                        composable("manager_dashboard") {
                            ManagerDashboardScreen()
                        }
                    }
                }
            }
        }
    }
}