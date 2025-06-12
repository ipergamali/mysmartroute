package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    val saveChecked = remember { mutableStateOf(false) }

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
            Button(onClick = { navController.navigate("themePicker") }) {
                Text("Επιλογή θέματος")
            }
            Button(onClick = { navController.navigate("fontPicker") }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Επιλογή γραμματοσειράς")
            }
            Button(onClick = { navController.navigate("soundPicker") }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Επιλογή ήχου")
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Checkbox(
                    checked = saveChecked.value,
                    onCheckedChange = { saveChecked.value = it }
                )
                Text("Οριστική αποθήκευση επιλογών", modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = {
                    if (saveChecked.value) {
                        viewModel.saveCurrentSettings(context)
                    } else {
                        Toast.makeText(context, "Επιλέξτε το κουτάκι πριν πατήσετε Apply", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Apply")
            }
        }
    }
}
