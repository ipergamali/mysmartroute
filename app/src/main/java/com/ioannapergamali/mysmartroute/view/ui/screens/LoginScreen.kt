package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.movewise.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: () -> Unit,
    onLoginFailure: (String) -> Unit = {}
) {
    val viewModel: AuthenticationViewModel = viewModel()
    val uiState by viewModel.loginState.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBar(
                title = "Login",
                navController = navController
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthenticationViewModel.LoginState.Success -> {
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            }
            is AuthenticationViewModel.LoginState.Error -> {
                val message = (uiState as AuthenticationViewModel.LoginState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                onLoginFailure(message)
            }
            else -> {}
        }
    }
}
