package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterVehicleScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: VehicleViewModel = viewModel()
    val state by viewModel.registerState.collectAsState()
    val context = LocalContext.current

    var description by remember { mutableStateOf("") }
    var seatInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(VehicleType.CAR) }

    Scaffold(
        topBar = {
            TopBar(
                title = "Register Vehicle",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = type.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    VehicleType.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                type = option
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = seatInput,
                onValueChange = { seatInput = it },
                label = { Text("Seats") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            if (state is VehicleViewModel.RegisterState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (state as VehicleViewModel.RegisterState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val seat = seatInput.toIntOrNull() ?: 0
                viewModel.registerVehicle(context, description, type, seat)
            }) {
                Text("Register")
            }
        }
    }

    LaunchedEffect(state) {
        if (state is VehicleViewModel.RegisterState.Success) {
            Toast.makeText(
                context,
                "Vehicle registered successfully",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }
    }
}
