package com.example.medicaladherence.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.medicaladherence.ui.screens.AddEditMedicationScreen
import com.example.medicaladherence.ui.screens.HomeScreen
import com.example.medicaladherence.ui.screens.SettingsScreen
import com.example.medicaladherence.ui.screens.StatsScreen

/**
 * Navigation routes
 */
object Routes {
    const val HOME = "home"
    const val ADD_MEDICATION = "add_medication"
    const val STATS = "stats"
    const val SETTINGS = "settings"
}

/**
 * Navigation graph for the Medical Adherence app
 */
@Composable
fun MedicalAdherenceNavGraph(
    navController: NavHostController,
    onFontScaleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToAdd = { navController.navigate(Routes.ADD_MEDICATION) },
                onNavigateToStats = { navController.navigate(Routes.STATS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ADD_MEDICATION) {
            AddEditMedicationScreen(
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
                onFontScaleChanged = onFontScaleChanged
            )
        }
    }
}
