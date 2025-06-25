package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ioannapergamali.mysmartroute.BuildConfig
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.view.ui.components.LogoImage
import com.ioannapergamali.mysmartroute.view.ui.components.LogoAssets

@Composable
fun AboutScreen(navController: NavController, openDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.about_title),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                LogoImage(
                    drawableRes = LogoAssets.COMPANY,
                    contentDescription = "Company logo"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.credits),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Developer: Ιωάννα Περγάμαλη")
                Text("Version: ${BuildConfig.VERSION_NAME}")

                Spacer(modifier = Modifier.height(8.dp))

                Text("Company: JOPE")
                Text("Address: Πάροδος Κρήτης 8, Γάζι, ΤΚ 71414")
                Text("Repository: https://github.com/ipergamali/mysmartroute.git")
            }
        }
    }
}
