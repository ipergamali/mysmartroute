package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.WalkingUtils
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import kotlin.time.Duration

@Composable
fun DefineDurationScreen(navController: NavController, openDrawer: () -> Unit) {
    var distanceText by rememberSaveable { mutableStateOf("") }
    var duration: Duration? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.define_duration),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text(stringResource(R.string.distance_meters)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                distanceText.toDoubleOrNull()?.let {
                    duration = WalkingUtils.walkingDuration(it)
                }
            }) {
                Text(stringResource(R.string.calculate))
            }

            duration?.let {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.walking_duration_result, it.toString()))
            }
        }
    }
}

