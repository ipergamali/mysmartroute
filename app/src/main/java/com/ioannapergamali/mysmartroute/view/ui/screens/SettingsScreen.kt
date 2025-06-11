package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun SettingsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTheme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
    val themes = AppTheme.values()

    Scaffold(
        topBar = {
            TopBar(
                title = "Settings",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Επιλογή θέματος")
            LazyColumn {
                items(themes) { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { ThemePreferenceManager.setTheme(context, theme) }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = {
                                scope.launch { ThemePreferenceManager.setTheme(context, theme) }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(theme.label)
                    }
                }
            }
        }
    }
}
