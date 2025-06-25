package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

@Composable
fun DatabaseMenuScreen(navController: NavController, openDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.databases_title),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Button(onClick = { navController.navigate("localDb") }) {
                Text(stringResource(R.string.local_db))
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = { navController.navigate("firebaseDb") }) {
                Text(stringResource(R.string.firebase_db))
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = { navController.navigate("syncDb") }) {
                Text(stringResource(R.string.sync_db))
            }
        }
    }
}
