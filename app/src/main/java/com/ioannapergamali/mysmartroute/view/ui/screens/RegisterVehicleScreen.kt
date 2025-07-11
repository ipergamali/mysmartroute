package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.classes.vehicles.RemoteVehicle
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterVehicleScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: VehicleViewModel = viewModel()
    val state by viewModel.registerState.collectAsState()
    val available by viewModel.availableVehicles.collectAsState()
    val context = LocalContext.current

    var description by remember { mutableStateOf("") }
    var seatInput by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VehicleType.CAR) }

    LaunchedEffect(Unit) { viewModel.loadAvailableVehicles(context) }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.register_vehicle),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues), scrollable = false) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VehicleType.values()) { option ->
                    val selected = option == type
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { type = option },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = when (option) {
                                    VehicleType.CAR -> Icons.Filled.DirectionsCar
                                    VehicleType.TAXI -> Icons.Filled.LocalTaxi
                                    VehicleType.BIGBUS -> Icons.Filled.DirectionsBus
                                    VehicleType.SMALLBUS -> Icons.Filled.AirportShuttle
                                    VehicleType.BICYCLE -> Icons.Filled.DirectionsBike
                                    VehicleType.MOTORBIKE -> Icons.Filled.TwoWheeler
                                },
                                contentDescription = option.name
                            )
                        }
                        Text(
                            text = when (option) {
                                VehicleType.CAR -> "Car"
                                VehicleType.TAXI -> "Taxi"
                                VehicleType.BIGBUS -> "Bus"
                                VehicleType.SMALLBUS -> "Small Bus"
                                VehicleType.BICYCLE -> "Bicycle"
                                VehicleType.MOTORBIKE -> "Motorbike"
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
