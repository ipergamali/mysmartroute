package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import com.ioannapergamali.mysmartroute.view.ui.MysmartrouteTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager
import com.ioannapergamali.mysmartroute.utils.FontPreferenceManager
import com.ioannapergamali.mysmartroute.data.ThemeLoader
import com.ioannapergamali.mysmartroute.data.ThemeOption
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    val currentTheme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
    val currentDark by ThemePreferenceManager.darkThemeFlow(context).collectAsState(initial = false)
    val currentFont by FontPreferenceManager.fontFlow(context).collectAsState(initial = AppFont.SansSerif)

    val customThemes = remember { ThemeLoader.load(context) }
    val themeOptions = remember(customThemes) { AppTheme.values().toList<ThemeOption>() + customThemes }

    var expanded by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf<ThemeOption>(currentTheme) }
    var dark by remember { mutableStateOf(currentDark) }

    LaunchedEffect(currentTheme) { selectedTheme = currentTheme }
    LaunchedEffect(currentDark) { dark = currentDark }

    MysmartrouteTheme(theme = selectedTheme, darkTheme = dark, font = currentFont.fontFamily) {
        Scaffold(
            topBar = {
                TopBar(title = "Themes", navController = navController)
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            ScreenContainer(modifier = Modifier.padding(padding)) {
                Text("Themes")
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedTheme.label,
                        onValueChange = {},
                        label = { Text("Theme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        themeOptions.forEach { theme ->
                            DropdownMenuItem(text = { Text(theme.label) }, onClick = {
                                selectedTheme = theme
                                expanded = false
                            })
                        }
                    }
                }
                Text("Dark Theme")
                Switch(checked = dark, onCheckedChange = {
                    dark = it
                })

                Button(
                    onClick = {
                        viewModel.applyTheme(context, selectedTheme, dark)
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
