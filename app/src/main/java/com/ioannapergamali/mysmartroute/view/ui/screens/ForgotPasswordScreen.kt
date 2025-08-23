package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.PasswordResetUtils
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    openDrawer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.reset_password_title),
                navController = navController,
                showMenu = true,
                showBack = true,
                showHomeIcon = false,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        val context = LocalContext.current
        var email by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { PasswordResetUtils.sendPasswordResetEmail(email, context) }) {
                Text(stringResource(R.string.send_reset_email))
            }
        }
    }
}

