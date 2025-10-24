package com.example.medicaladherence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medicaladherence.ui.nav.Routes
import com.example.medicaladherence.ui.screens.*
import com.example.medicaladherence.ui.theme.MedicalAdherenceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalAdherenceApp()
        }
    }
}

@Composable
fun MedicalAdherenceApp() {
    val navController = rememberNavController()
    var fontScale by remember { mutableFloatStateOf(1.0f) }

    MedicalAdherenceTheme(fontScale = fontScale) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Define which routes should show bottom bar
        val routesWithBottomBar = listOf(
            Routes.HOME,
            Routes.MEDICATIONS,
            Routes.STATS,
            Routes.SETTINGS
        )
        val showBottomBar = currentRoute in routesWithBottomBar

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == Routes.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home") },
                            selected = currentRoute == Routes.HOME,
                            onClick = {
                                if (currentRoute != Routes.HOME) {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = {
                                Text(
                                    text = "💊",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            label = { Text("Medications") },
                            selected = currentRoute == Routes.MEDICATIONS,
                            onClick = {
                                if (currentRoute != Routes.MEDICATIONS) {
                                    navController.navigate(Routes.MEDICATIONS) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = {
                                Text(
                                    text = "📊",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            label = { Text("Stats") },
                            selected = currentRoute == Routes.STATS,
                            onClick = {
                                if (currentRoute != Routes.STATS) {
                                    navController.navigate(Routes.STATS) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == Routes.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") },
                            selected = currentRoute == Routes.SETTINGS,
                            onClick = {
                                if (currentRoute != Routes.SETTINGS) {
                                    navController.navigate(Routes.SETTINGS) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateToAdd = { navController.navigate(Routes.ADD_MEDICATION) },
                        onNavigateToEdit = { medId ->
                            navController.navigate("add_medication?id=$medId")
                        }
                    )
                }

                composable(Routes.MEDICATIONS) {
                    MedicationsLibraryScreen(
                        onNavigateToAdd = { navController.navigate(Routes.ADD_MEDICATION) },
                        onNavigateToEdit = { medId ->
                            navController.navigate("add_medication?id=$medId")
                        }
                    )
                }

                composable(
                    route = "add_medication?id={medId}",
                    arguments = listOf(
                        navArgument("medId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val medId = backStackEntry.arguments?.getString("medId")
                    AddEditMedicationScreen(
                        medicationId = medId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.STATS) {
                    StatsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFontScaleChanged = { newScale -> fontScale = newScale }
                    )
                }
            }
        }
    }
}