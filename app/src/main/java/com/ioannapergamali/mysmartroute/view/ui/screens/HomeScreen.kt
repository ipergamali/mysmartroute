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
import com.ioannapergamali.movewise.ui.components.TopBar
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.animation.rememberBreathingAnimation
import com.ioannapergamali.mysmartroute.view.ui.animation.rememberTextPulseAnimation

@Composable
fun HomeScreen(
    navController: NavController,
    onNavigateToSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "Home",
                navController = navController
            )
        }
    ) { paddingValues ->

        val contentModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)

        val (logoScale, logoAlpha) = rememberBreathingAnimation()
        val (textScale, textAlpha) = rememberTextPulseAnimation()

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = textScale
                        scaleY = textScale
                        this.alpha = textAlpha
                    }
            )

            Image(
                painter = painterResource(id = R.drawable.sr),
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

            Button(onClick = { onNavigateToLogin() }) {
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
}
