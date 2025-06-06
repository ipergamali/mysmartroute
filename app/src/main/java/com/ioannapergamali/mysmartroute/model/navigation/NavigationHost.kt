package com.ioannapergamali.mysmartroute.model.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ioannapergamali.mysmartroute.view.ui.screens.HomeScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SignUpScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.LoginScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.MenuScreen
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole



@Composable
fun NavigationHost(navController : NavHostController) {

    val context = LocalContext.current


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
                onLoginSuccess = { role ->
                    val destination = when (role) {
                        UserRole.DRIVER -> "menu/DRIVER"
                        UserRole.PASSENGER -> "menu/PASSENGER"
                        UserRole.ADMIN -> "menu/ADMIN"
                    }
                    navController.navigate(destination) {
                        popUpTo("login") { inclusive = true }
                    }
                } ,
                onLoginFailure = { errorMessage ->
                    Toast.makeText(context , errorMessage , Toast.LENGTH_SHORT).show()
                }
            )
        }



        composable("Signup") {
            SignUpScreen(
                navController = navController ,
                onSignUpSuccess = {
                    Toast.makeText(
                        context,
                        "Sign up successful! Please verify your email and log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                } ,
                onSignUpFailure = { errorMessage ->
                    Toast.makeText(context , errorMessage , Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable("menu/{role}") { backStackEntry ->
            val roleArg = backStackEntry.arguments?.getString("role") ?: UserRole.PASSENGER.name
            val role = try {
                UserRole.valueOf(roleArg)
            } catch (_: IllegalArgumentException) {
                UserRole.PASSENGER
            }
            MenuScreen(navController = navController, role = role)
        }



    }
}
