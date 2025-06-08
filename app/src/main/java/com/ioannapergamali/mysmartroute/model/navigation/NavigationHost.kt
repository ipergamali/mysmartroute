package com.ioannapergamali.mysmartroute.model.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ioannapergamali.mysmartroute.view.ui.screens.HomeScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SignUpScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.LoginScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.MenuScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RegisterVehicleScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AnnounceTransportScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DirectionsMapScreen



@Composable
fun NavigationHost(navController : NavHostController) {

    NavHost(navController = navController , startDestination = "home") {

        composable("home") {
            HomeScreen(
                navController = navController ,
                onNavigateToSignUp = {
                    navController.navigate("Signup")
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }

        composable("login") {
            LoginScreen(
                navController = navController ,
                onLoginSuccess = {
                    navController.navigate("menu") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }



        composable("Signup") {
            SignUpScreen(
                navController = navController ,
                onSignUpSuccess = {
                    navController.navigate("menu") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }
        composable("menu") {
            MenuScreen(navController = navController)
        }

        composable("registerVehicle") {
            RegisterVehicleScreen(navController = navController)
        }

        composable("announceTransport") {
            AnnounceTransportScreen(navController = navController)
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
            DirectionsMapScreen(navController = navController, start = start, end = end)
        }



    }
}
