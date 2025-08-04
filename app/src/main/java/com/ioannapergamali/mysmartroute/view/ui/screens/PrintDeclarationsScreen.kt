package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrintDeclarationsScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: TransportDeclarationViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val declarations by viewModel.declarations.collectAsState()
    val role by authViewModel.currentUserRole.collectAsState()
    val context = LocalContext.current
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUserRole(context)
    }

    LaunchedEffect(role) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (role == UserRole.ADMIN) {
            viewModel.loadDeclarations(context)
        } else if (role != null && uid != null) {
            viewModel.loadDeclarations(context, uid)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.print_declarations),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        if (declarations.isEmpty()) {
            Text(
                text = stringResource(R.string.no_declarations),
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
            ) {
                val hasSelection = selected.values.any { it }
                Button(
                    onClick = {
                        val ids = selected.filterValues { it }.keys
                        viewModel.deleteDeclarations(context, ids.toSet())
                        ids.forEach { selected.remove(it) }
                    },
                    enabled = hasSelection
                ) {
                    Text(stringResource(R.string.delete_selected))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(declarations) { decl ->
                        val isChecked = selected[decl.id] ?: false
                        DeclarationItem(
                            declaration = decl,
                            isChecked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selected[decl.id] = true
                                } else {
                                    selected.remove(decl.id)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeclarationItem(
    declaration: TransportDeclarationEntity,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var routeName by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var vehicleName by remember { mutableStateOf("") }

    LaunchedEffect(declaration) {
        val db = MySmartRouteDatabase.getInstance(context)
        routeName = db.routeDao().findById(declaration.routeId)?.name.orEmpty()
        val driver = db.userDao().getUser(declaration.driverId)
        driverName = listOfNotNull(driver?.name, driver?.surname).joinToString(" ").trim()
        vehicleName = db.vehicleDao().getVehicle(declaration.vehicleId)?.name.orEmpty()
    }

    val vehicleIcon = when (VehicleType.valueOf(declaration.vehicleType)) {
        VehicleType.CAR, VehicleType.TAXI -> Icons.Filled.DirectionsCar
        VehicleType.BIGBUS, VehicleType.SMALLBUS -> Icons.Filled.DirectionsBus
        VehicleType.BICYCLE -> Icons.Filled.DirectionsBike
        VehicleType.MOTORBIKE -> Icons.Filled.TwoWheeler
    }

    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateText = formatter.format(Date(declaration.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "${stringResource(R.string.route)}: ${routeName.ifBlank { declaration.routeId }}")
                Text(text = "${stringResource(R.string.driver)}: ${driverName.ifBlank { declaration.driverId }}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(vehicleIcon, contentDescription = stringResource(R.string.vehicle))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = vehicleName.ifBlank { declaration.vehicleType })
                }
                Text(text = "${stringResource(R.string.cost)}: ${declaration.cost}")
                Text(text = "${stringResource(R.string.date)}: $dateText")
            }
        }
    }
}
