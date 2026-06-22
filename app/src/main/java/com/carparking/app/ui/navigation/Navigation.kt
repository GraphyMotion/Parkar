package com.carparking.app.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.carparking.app.ui.screens.*

sealed class Screen(val route: String) {
    object Onboarding    : Screen("onboarding")
    object Home          : Screen("home")
    object CarList       : Screen("car_list")
    object AddCar        : Screen("add_car")
    object EditCar       : Screen("edit_car/{carId}") {
        fun createRoute(carId: Int) = "edit_car/$carId"
    }
    object SaveParking   : Screen("save_parking/{carId}") {
        fun createRoute(carId: Int) = "save_parking/$carId"
    }
    object ParkingDetail : Screen("parking_detail/{carId}") {
        fun createRoute(carId: Int) = "parking_detail/$carId"
    }
    object ParkingHistory : Screen("parking_history/{carId}") {
        fun createRoute(carId: Int) = "parking_history/$carId"
    }
    object BluetoothSettings : Screen("bluetooth_settings")
}

private const val PREFS_NAME   = "car_parking_prefs"
private const val KEY_ONBOARDING = "onboarding_done"

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current

    // Vérifie si c'est le premier lancement
    val isFirstLaunch = remember {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        !prefs.getBoolean(KEY_ONBOARDING, false)
    }

    val startDestination = if (isFirstLaunch) Screen.Onboarding.route else Screen.Home.route

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Onboarding (premier lancement uniquement) ────────────────────────
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    // Marque l'onboarding comme terminé
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_ONBOARDING, true)
                        .apply()

                    navController.navigate(Screen.Home.route) {
                        // Retire l'onboarding de la back stack (bouton Retour → pas de retour)
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Écrans principaux ─────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.CarList.route) {
            CarListScreen(navController = navController)
        }
        composable(Screen.AddCar.route) {
            AddEditCarScreen(navController = navController, carId = null)
        }
        composable(
            route = Screen.EditCar.route,
            arguments = listOf(navArgument("carId") { type = NavType.IntType })
        ) { back ->
            AddEditCarScreen(
                navController = navController,
                carId = back.arguments?.getInt("carId")
            )
        }
        composable(
            route = Screen.SaveParking.route,
            arguments = listOf(navArgument("carId") { type = NavType.IntType })
        ) { back ->
            SaveParkingScreen(
                navController = navController,
                carId = back.arguments?.getInt("carId") ?: 0
            )
        }
        composable(
            route = Screen.ParkingDetail.route,
            arguments = listOf(navArgument("carId") { type = NavType.IntType })
        ) { back ->
            ParkingDetailScreen(
                navController = navController,
                carId = back.arguments?.getInt("carId") ?: 0
            )
        }
        composable(
            route = Screen.ParkingHistory.route,
            arguments = listOf(navArgument("carId") { type = NavType.IntType })
        ) { back ->
            ParkingHistoryScreen(
                navController = navController,
                carId = back.arguments?.getInt("carId") ?: 0
            )
        }
        composable(Screen.BluetoothSettings.route) {
            BluetoothSettingsScreen(navController = navController)
        }
    }
}
