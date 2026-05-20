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
import com.example.shipmonitoring.ui.screens.SplashScreen
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

                    NavHost(
                        navController = navController,
                        // UBAH startDestination MENJADI "splash"
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // 1. Rute Splash Screen
                        composable("splash") {
                            SplashScreen(
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        // Hancurkan splash screen dari memori agar user tidak bisa 'Back' ke splash
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Rute Layar Login
                        composable("login") {
                            val authViewModel: AuthViewModel = viewModel()
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = { user ->
                                    when (user.role.uppercase()) {
                                        "NAHKODA" -> {
                                            // Masukkan shipId ke dalam route navigasi
                                            val shipId = user.shipId ?: "unknown"
                                            navController.navigate("nahkoda_dashboard/$shipId") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                        "MANAGER" -> {
                                            navController.navigate("manager_dashboard") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // 3. Rute Layar Dashboard Nahkoda (Terima Argumen)
                        composable("nahkoda_dashboard/{shipId}") { backStackEntry ->
                            // Ekstrak shipId dari parameter navigasi
                            val shipId = backStackEntry.arguments?.getString("shipId") ?: ""
                            val nahkodaViewModel: NahkodaViewModel = viewModel()

                            NahkodaDashboardScreen(
                                viewModel = nahkodaViewModel,
                                shipId = shipId,
                                onLogout = {
                                    // Hentikan pelacakan GPS jika logout!
                                    nahkodaViewModel.stopLiveLocation()
                                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                }
                            )
                        }

                        // 4. Rute Layar Dashboard Manager
                        composable("manager_dashboard") {
                            ManagerDashboardScreen(
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}