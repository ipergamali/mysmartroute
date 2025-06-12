package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.animation.rememberBreathingAnimation
import com.ioannapergamali.mysmartroute.view.ui.animation.rememberSlideFadeInAnimation
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer

@Composable
fun HomeScreen(
    navController: NavController,
    onNavigateToSignUp: () -> Unit,
    openDrawer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "Home",
                navController = navController,
                showMenu = true,
                showBack = false,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->

        val (logoScale, logoAlpha) = rememberBreathingAnimation()
        val (textOffset, textAlpha) = rememberSlideFadeInAnimation()

        val viewModel: AuthenticationViewModel = viewModel()
        val uiState by viewModel.loginState.collectAsState()
        val context = LocalContext.current
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .offset(y = textOffset)
                    .graphicsLayer {
                        this.alpha = textAlpha
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Animated Logo",
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        this.alpha = logoAlpha
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState is AuthenticationViewModel.LoginState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (uiState as AuthenticationViewModel.LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = { viewModel.login(email, password) }) {
                Text("Login")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text("If you don't have account ")
                Text(
                    text = "Sign Up",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthenticationViewModel.LoginState.Success -> {
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                navController.navigate("menu") {
                    popUpTo("home") { inclusive = true }
                }
            }
            is AuthenticationViewModel.LoginState.Error -> {
                val message = (uiState as AuthenticationViewModel.LoginState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
}
