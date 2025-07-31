package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.util.iconForVehicle
import com.ioannapergamali.mysmartroute.view.ui.util.labelForVehicle
import com.ioannapergamali.mysmartroute.viewmodel.FavoritesViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp

@Composable
private fun TableCell(width: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) { content() }
}

@Composable
private fun HeaderRow() {
    Row {
        TableCell(width = 40.dp) { Text("") }
        TableCell(width = 140.dp) { Text(stringResource(R.string.driver)) }
        TableCell(width = 120.dp) { Text(stringResource(R.string.vehicle_name)) }
        TableCell(width = 120.dp) { Text(stringResource(R.string.vehicle_type)) }
        TableCell(width = 80.dp) { Text(stringResource(R.string.cost)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableTransportsScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    routeId: String?,
    startId: String?,
    endId: String?
) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val favoritesViewModel: FavoritesViewModel = viewModel()

    val declarations by declarationViewModel.declarations.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val preferred by favoritesViewModel.preferredFlow(context).collectAsState(initial = emptySet())
    val nonPreferred by favoritesViewModel.nonPreferredFlow(context).collectAsState(initial = emptySet())

    val pois = remember { mutableStateListOf<PoIEntity>() }
    var startIndex by remember { mutableStateOf(-1) }
    var endIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        declarationViewModel.loadDeclarations(context)
        userViewModel.loadDrivers(context)
        vehicleViewModel.loadRegisteredVehicles(context, includeAll = true)
    }

    LaunchedEffect(routeId) {
        if (routeId != null) {
            pois.clear()
            pois.addAll(routeViewModel.getRoutePois(context, routeId))
            startIndex = pois.indexOfFirst { it.id == startId }
            endIndex = pois.indexOfFirst { it.id == endId }
        }
    }

    val driverNames = drivers.associate { it.id to "${'$'}{it.name} ${'$'}{it.surname}" }
    val vehiclesByDriver = vehicles.groupBy { it.userId }
    val list = declarations.filter { decl ->
        if (decl.routeId != routeId) return@filter false
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) return@filter false
        val type = runCatching { VehicleType.valueOf(decl.vehicleType) }.getOrNull()
        type == null || !nonPreferred.contains(type)
    }


    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.available_transports),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (list.isEmpty()) {
                Text(stringResource(R.string.no_transports_found))
            } else {
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.horizontalScroll(scrollState)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item { HeaderRow() }
                        items(list) { decl ->
                            val driver = driverNames[decl.driverId] ?: ""
                            val type = runCatching { VehicleType.valueOf(decl.vehicleType) }.getOrNull()
                            val preferredType = type != null && preferred.contains(type)
                            val vehicleName = vehiclesByDriver[decl.driverId]
                                ?.firstOrNull { runCatching { VehicleType.valueOf(it.type) }.getOrNull() == type }
                                ?.name ?: ""
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(width = 40.dp) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (preferredType) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        type?.let {
                                            Icon(
                                                imageVector = iconForVehicle(it),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                                TableCell(width = 140.dp) { Text(driver, softWrap = false) }
                                TableCell(width = 120.dp) { Text(vehicleName, softWrap = false) }
                                TableCell(width = 120.dp) { Text(type?.let { labelForVehicle(it) } ?: "", softWrap = false) }
                                TableCell(width = 80.dp) { Text(decl.cost.toString(), softWrap = false) }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
