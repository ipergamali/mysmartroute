package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import com.ioannapergamali.mysmartroute.view.ui.util.rememberWindowInfo
import com.ioannapergamali.mysmartroute.view.ui.util.WindowOrientation
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.LogoImage
import com.ioannapergamali.mysmartroute.view.ui.components.LogoAssets
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

        val windowInfo = rememberWindowInfo()

        val (logoScale, logoAlpha) = rememberBreathingAnimation()
        val (textOffset, textAlpha) = rememberSlideFadeInAnimation()

        val viewModel: AuthenticationViewModel = viewModel()
        val uiState by viewModel.loginState.collectAsState()
        val context = LocalContext.current
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            val isLarge = windowInfo.width > 600.dp && windowInfo.orientation == WindowOrientation.Landscape
            val containerModifier = if (isLarge) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
            val arrangement: Arrangement.Vertical = if (isLarge) Arrangement.Center else Arrangement.Top
            val alignment: Alignment.Horizontal = Alignment.CenterHorizontally
            if (isLarge) {
                Row(
                    modifier = containerModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HomeContent(
                        logoScale = logoScale,
                        logoAlpha = logoAlpha,
                        textOffset = textOffset,
                        textAlpha = textAlpha,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        uiState = uiState,
                        onLogin = { viewModel.login(email, password) },
                        onNavigateToSignUp = onNavigateToSignUp,
                        onResendVerification = { viewModel.resendVerificationEmail() },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = containerModifier,
                    verticalArrangement = arrangement,
                    horizontalAlignment = alignment
                ) {
                    HomeContent(
                        logoScale = logoScale,
                        logoAlpha = logoAlpha,
                        textOffset = textOffset,
                        textAlpha = textAlpha,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        uiState = uiState,
                        onLogin = { viewModel.login(email, password) },
                        onNavigateToSignUp = onNavigateToSignUp,
                        onResendVerification = { viewModel.resendVerificationEmail() }
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
                    // Άνοιγμα του πλαϊνού μενού ώστε να εμφανιστεί η επιλογή "Settings"
                    openDrawer()
                }
                is AuthenticationViewModel.LoginState.Error -> {
                    val message = (uiState as AuthenticationViewModel.LoginState.Error).message
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                is AuthenticationViewModel.LoginState.EmailVerificationSent -> {
                    Toast.makeText(context, "Το email επιβεβαίωσης στάλθηκε", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun HomeContent(
    logoScale: Float,
    logoAlpha: Float,
    textOffset: Dp,
    textAlpha: Float,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    uiState: AuthenticationViewModel.LoginState,
    onLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onResendVerification: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .offset(y = textOffset)
                .graphicsLayer { this.alpha = textAlpha }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LogoImage(
            drawableRes = LogoAssets.LOGO,
            contentDescription = "Animated Logo",
            modifier = Modifier.graphicsLayer {
                scaleX = logoScale
                scaleY = logoScale
                this.alpha = logoAlpha
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

    if (uiState is AuthenticationViewModel.LoginState.Error) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiState.message,
            color = MaterialTheme.colorScheme.error
        )
    }

    if (uiState is AuthenticationViewModel.LoginState.EmailNotVerified) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onResendVerification) {
            Text("Αποστολή ξανά email επιβεβαίωσης")
        }
    }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onLogin) {
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
