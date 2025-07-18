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
import com.ioannapergamali.mysmartroute.view.ui.screens.DeclareRouteScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DirectionsMapScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PoIListScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DefinePoiScreen
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
import com.ioannapergamali.mysmartroute.view.ui.screens.ProfileScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RouteEditorScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ManageFavoritesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintCompletedScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintListScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintScheduledScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RouteModeScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ViewVehiclesScreen



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

        composable("declareRoute") {
            DeclareRouteScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("announceAvailability") {
            AnnounceTransportScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("poiList") {
            PoIListScreen(navController = navController, openDrawer = openDrawer)
        }
        composable("editRoute") {
            RouteEditorScreen(navController = navController, openDrawer = openDrawer)
        }
        composable(
            route = "definePoi?lat={lat}&lng={lng}&source={source}&view={view}",
            arguments = listOf(
                navArgument("lat") { defaultValue = "" },
                navArgument("lng") { defaultValue = "" },
                navArgument("source") { defaultValue = "" },
                navArgument("view") { defaultValue = "false" }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
            val source = backStackEntry.arguments?.getString("source")
            val viewOnly = backStackEntry.arguments?.getString("view")?.toBoolean() ?: false
            DefinePoiScreen(
                navController = navController,
                openDrawer = openDrawer,
                initialLat = lat,
                initialLng = lng,
                source = source,
                viewOnly = viewOnly
            )
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

        composable("profile") {
            ProfileScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("manageFavorites") {
            ManageFavoritesScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("routeMode") {
            RouteModeScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("printList") {
            PrintListScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("printScheduled") {
            PrintScheduledScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("printCompleted") {
            PrintCompletedScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("viewVehicles") {
            ViewVehiclesScreen(navController = navController, openDrawer = openDrawer)
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






    }
}
