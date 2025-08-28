package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble

@Composable
fun ResetPasswordScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: AuthenticationViewModel = viewModel()
    val uiState by viewModel.resetPasswordState.collectAsState()
    var email by remember { mutableStateOf("") }
    val bubbleState = LocalKeyboardBubbleState.current!!


    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.reset_password),
                navController = navController,
                showMenu = false,
                showHomeIcon = false,
                showLanguageToggle = false,
                showNotifications = false
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 0) { email }
                )
                Spacer(modifier = Modifier.height(16.dp))
     Text(stringResource(R.string.send_reset_email))
                }
                when (uiState) {
                    is AuthenticationViewModel.ResetPasswordState.Loading -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                    is AuthenticationViewModel.ResetPasswordState.Success -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.password_reset_email_sent))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.clearResetPasswordState()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }) {
                            Text(stringResource(R.string.back_to_login))
                        }
                    }
                    is AuthenticationViewModel.ResetPasswordState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            (uiState as AuthenticationViewModel.ResetPasswordState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

