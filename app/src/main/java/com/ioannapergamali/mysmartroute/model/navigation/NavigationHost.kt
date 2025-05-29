package com.ioannapergamali.mysmartroute.model.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ioannapergamali.mysmartroute.view.ui.screens.HomeScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SignUpScreen



@Composable
fun NavigationHost(navController : NavHostController) {

    val context = LocalContext.current


    NavHost(navController = navController , startDestination = "home") {

        composable("home") {
            HomeScreen(
                navController = navController ,
                onNavigateToSignUp = {
                    navController.navigate("Signup")
                }
            )
        }
//        // Login Screen
//        composable("login") {
//            LoginScreen(
//                navController = navController ,
//                onLoginSuccess = {
//                    navController.navigate("menu") {
//                        popUpTo("login") { inclusive = true }
//                    }
//                } ,
//                onLoginFailure = { errorMessage ->
//                    Toast.makeText(context , errorMessage , Toast.LENGTH_SHORT).show()
//                } ,
//                onNavigateToSettings = {
//                    navController.navigate("settings")
//                }
//            )
//        }



        composable("Signup") {
            SignUpScreen(
                navController = navController ,
                onSignUpSuccess = {
                    navController.navigate("menu") {
                        popUpTo("signup") { inclusive = true }
                    }
                } ,
                onSignUpFailure = { errorMessage ->
                    Toast.makeText(context , errorMessage , Toast.LENGTH_SHORT).show()
                }
            )
        }



    }
}
