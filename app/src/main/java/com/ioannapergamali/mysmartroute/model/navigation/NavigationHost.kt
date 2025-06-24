package com.ioannapergamali.mysmartroute.model.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ioannapergamali.mysmartroute.view.ui.screens.HomeScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SignUpScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.MenuScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RegisterVehicleScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AnnounceTransportScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DirectionsMapScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PoIListScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SettingsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AboutScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SupportScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ThemePickerScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.FontPickerScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SoundPickerScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DatabaseMenuScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.LocalDatabaseScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.FirebaseDatabaseScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AdminSignUpScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DatabaseSyncScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RolesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AdminTemplateScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.MaterializeAdminScreen



@Composable
fun NavigationHost(navController : NavHostController, openDrawer: () -> Unit) {

    NavHost(navController = navController , startDestination = "home") {

        composable("home") {
            HomeScreen(
                navController = navController ,
                onNavigateToSignUp = {
                    navController.navigate("Signup")
                },
                openDrawer = openDrawer
            )
        }



        composable("Signup") {
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = {
                    navController.navigate("home") {
                        popUpTo("Signup") { inclusive = true }
                    }
                },
                openDrawer = openDrawer
            )
        }
        composable("menu") {
            MenuScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("registerVehicle") {
            RegisterVehicleScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("announceTransport") {
            AnnounceTransportScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("announceAvailability") {
            AnnounceTransportScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("poiList") {
            PoIListScreen(navController = navController, openDrawer = openDrawer)
        }

        composable(
            route = "directionsMap/{start}/{end}",
            arguments = listOf(
                navArgument("start") { defaultValue = "" },
                navArgument("end") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val start = backStackEntry.arguments?.getString("start") ?: ""
            val end = backStackEntry.arguments?.getString("end") ?: ""
            DirectionsMapScreen(navController = navController, start = start, end = end, openDrawer = openDrawer)
        }

        composable("settings") {
            SettingsScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("themePicker") {
            ThemePickerScreen(navController = navController)
        }

        composable("fontPicker") {
            FontPickerScreen(navController = navController)
        }

        composable("roles") {
            RolesScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("soundPicker") {
            SoundPickerScreen(navController = navController)
        }

        composable("about") {
            AboutScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("support") {
            SupportScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("databaseMenu") {
            DatabaseMenuScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("localDb") {
            LocalDatabaseScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("firebaseDb") {
            FirebaseDatabaseScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("syncDb") {
            DatabaseSyncScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("adminSignup") {
            AdminSignUpScreen(
                navController = navController,
                onSignUpSuccess = {
                    navController.popBackStack()
                },
                openDrawer = openDrawer
            )
        }

        composable("adminDashboard") {
            AdminTemplateScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("materializeDashboard") {
            MaterializeAdminScreen(navController = navController, openDrawer = openDrawer)
        }




    }
}
