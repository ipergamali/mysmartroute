package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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

@Composable
fun HomeScreen(
    navController: NavController,
    onNavigateToSignUp: () -> Unit
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

        val (scale, alpha) = rememberBreathingAnimation()

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.sr),
                contentDescription = "Animated Logo",
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .padding(vertical = 8.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { onNavigateToSignUp() }) {
                    Text("Sign Up")
                }
            }
        }
    }
}
