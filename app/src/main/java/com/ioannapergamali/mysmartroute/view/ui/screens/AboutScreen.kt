package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar

@Composable
fun AboutScreen(navController: NavController, openDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            TopBar(
                title = "About",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Text("Developer: Ιωάννα Περγάμαλη")
            Text("Company: JOPE")
            Text("Address: Πάροδος Κρήτης 8, Γάζι, ΤΚ 71414")
            Text("Repository: https://github.com/ipergamali/mysmartroute.git")
        }
    }
}
