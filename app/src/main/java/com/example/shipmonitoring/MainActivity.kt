package com.example.shipmonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shipmonitoring.data.api.AppContainer
import com.example.shipmonitoring.ui.screens.SplashScreen
import com.example.shipmonitoring.ui.screens.admin.AdminDashboardScreen
import com.example.shipmonitoring.ui.screens.auth.AuthViewModel
import com.example.shipmonitoring.ui.screens.auth.LoginScreen
import com.example.shipmonitoring.ui.screens.manager.ManagerDashboardScreen
import com.example.shipmonitoring.ui.screens.nahkoda.NahkodaDashboardScreen
import com.example.shipmonitoring.ui.screens.nahkoda.NahkodaViewModel
import com.example.shipmonitoring.ui.theme.ShipMonitoringTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            ShipMonitoringTheme {
                // Inisialisasi NavController untuk mengatur perpindahan layar
                val navController = rememberNavController()
                val currentBackStackEntry = navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry.value?.destination?.route
                val session = AppContainer.sessionManager.sessionFlow.collectAsState(initial = null).value
                var isSessionResolved by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    AppContainer.sessionManager.sessionFlow.first()
                    isSessionResolved = true
                }

                LaunchedEffect(session, currentRoute, isSessionResolved) {
                    if (!isSessionResolved) {
                        return@LaunchedEffect
                    }

                    if (session != null) {
                        return@LaunchedEffect
                    }

                    if (currentRoute == null || currentRoute == "login" || currentRoute == "splash") {
                        return@LaunchedEffect
                    }

                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }

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
                                onNavigateNext = { destination ->
                                    navController.navigate(destination) {
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
                                            navController.navigate("nahkoda_dashboard") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                        "MANAGER" -> {
                                            navController.navigate("manager_dashboard") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                        "ADMIN" -> {
                                            navController.navigate("admin_dashboard") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                        else -> {
                                            navController.navigate("login") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // 3. Rute Layar Dashboard Nahkoda
                        composable("nahkoda_dashboard") {
                            val nahkodaViewModel: NahkodaViewModel = viewModel()

                            NahkodaDashboardScreen(
                                viewModel = nahkodaViewModel,
                                onLogout = {
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

                        composable("admin_dashboard") {
                            AdminDashboardScreen(
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
