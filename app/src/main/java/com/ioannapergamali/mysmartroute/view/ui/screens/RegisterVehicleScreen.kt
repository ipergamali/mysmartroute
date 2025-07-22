package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleColor
import androidx.compose.material3.menuAnchor
import com.ioannapergamali.mysmartroute.model.classes.vehicles.RemoteVehicle
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.ColorWheelPicker
import com.ioannapergamali.mysmartroute.view.ui.components.toHex
import com.ioannapergamali.mysmartroute.view.ui.components.toColorOrNull
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R
import androidx.compose.ui.graphics.Color
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterVehicleScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: VehicleViewModel = viewModel()
    val state by viewModel.registerState.collectAsState()
    val available by viewModel.availableVehicles.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    val descriptionOptions = listOf(
        "Αυτοκίνητο",
        "Ταξί",
        "Λεωφορείο",
        "Βαν",
        "Ποδήλατο",
        "Μηχανάκι"
    )
    var description by remember { mutableStateOf(descriptionOptions.first()) }
    var color by remember { mutableStateOf(VehicleColor.BLACK) }
    var usingCustomColor by remember { mutableStateOf(false) }
    var customColorName by remember { mutableStateOf("") }
    var customColorValue by remember { mutableStateOf(Color.Black) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var seat by remember { mutableStateOf(0) }
    var type by remember { mutableStateOf(VehicleType.CAR) }
    var colorExpanded by remember { mutableStateOf(false) }
    var descExpanded by remember { mutableStateOf(false) }

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
        ScreenContainer(
            modifier = Modifier.padding(paddingValues)
        ) {
            val bubbleState = LocalKeyboardBubbleState.current!!
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleType.values().forEach { option ->
                    val selected = option == type
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                type = option
                                description = when (option) {
                                    VehicleType.CAR -> "Αυτοκίνητο"
                                    VehicleType.TAXI -> "Ταξί"
                                    VehicleType.BIGBUS -> "Λεωφορείο"
                                    VehicleType.SMALLBUS -> "Βαν"
                                    VehicleType.BICYCLE -> "Ποδήλατο"
                                    VehicleType.MOTORBIKE -> "Μηχανάκι"
                                }
                            },
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
                                VehicleType.CAR -> "Αυτοκίνητο"
                                VehicleType.TAXI -> "Ταξί"
                                VehicleType.BIGBUS -> "Λεωφορείο"
                                VehicleType.SMALLBUS -> "Βαν"
                                VehicleType.BICYCLE -> "Ποδήλατο"
                                VehicleType.MOTORBIKE -> "Μηχανάκι"
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.vehicle_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 0) { name },
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it },
                label = { Text(stringResource(R.string.license_plate)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .observeBubble(bubbleState, 1) { plate },
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = descExpanded, onExpandedChange = {}) {
                OutlinedTextField(
                    value = description,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Περιγραφή") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = descExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(expanded = descExpanded, onDismissRequest = { descExpanded = false }) {
                    descriptionOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            description = option
                            descExpanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = colorExpanded, onExpandedChange = { colorExpanded = !colorExpanded }) {
                val currentLabel = if (usingCustomColor) customColorName else color.label
                val previewColor = if (usingCustomColor) customColorValue else color.color
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.vehicle_color)) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(previewColor, CircleShape)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(expanded = colorExpanded, onDismissRequest = { colorExpanded = false }) {
                    VehicleColor.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .background(option.color, CircleShape)
                                )
                            },
                            onClick = {
                                usingCustomColor = false
                                color = option
                                colorExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Custom") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        },
                        onClick = {
                            showCustomDialog = true
                            colorExpanded = false
                        }
                    )
                }
            }
            if (showCustomDialog) {
                var tempColor by remember { mutableStateOf(customColorValue) }
                var tempHex by remember { mutableStateOf(tempColor.toHex()) }
                AlertDialog(
                    onDismissRequest = { showCustomDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            customColorValue = tempColor
                            usingCustomColor = true
                            if (customColorName.isBlank()) customColorName = "Custom"
                            showCustomDialog = false
                        }) { Text("OK") }
                    },
                    title = { Text("Επιλογή χρώματος") },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ColorWheelPicker(
                                selectedColor = tempColor,
                                onColorSelected = {
                                    tempColor = it
                                    tempHex = it.toHex()
                                },
                                modifier = Modifier.size(220.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .background(tempColor, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = tempHex,
                                    onValueChange = {
                                        tempHex = it
                                        it.toColorOrNull()?.let { c -> tempColor = c }
                                    },
                                    label = { Text("HEX") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .observeBubble(bubbleState, 2) { tempHex }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customColorName,
                                onValueChange = { customColorName = it },
                                label = { Text("Όνομα χρώματος") },
                                singleLine = true,
                                modifier = Modifier.observeBubble(bubbleState, 3) { customColorName }
                            )
                        }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = seat.toString(),
                onValueChange = { seat = it.toIntOrNull() ?: seat },
                label = { Text(stringResource(R.string.seats_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .observeBubble(bubbleState, 4) { seat.toString() }
                    .width(160.dp),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { if (seat > 0) seat-- }) {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Decrease"
                            )
                        }
                        IconButton(onClick = { seat++ }) {
                            Icon(
                                Icons.Filled.ArrowDropUp,
                                contentDescription = "Increase"
                            )
                        }
                    }
                },
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
                val colorParam = if (usingCustomColor) customColorName else color.name
                viewModel.registerVehicle(context, name, description, type, seat, colorParam, plate)
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
