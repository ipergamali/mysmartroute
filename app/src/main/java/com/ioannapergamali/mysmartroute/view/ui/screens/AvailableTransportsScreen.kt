package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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
import com.ioannapergamali.mysmartroute.view.ui.util.labelForVehicle
import com.ioannapergamali.mysmartroute.viewmodel.FavoritesViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel

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
    val favoritesViewModel: FavoritesViewModel = viewModel()

    val declarations by declarationViewModel.declarations.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    val preferred by favoritesViewModel.preferredFlow(context).collectAsState(initial = emptySet())

    val pois = remember { mutableStateListOf<PoIEntity>() }
    var startIndex by remember { mutableStateOf(-1) }
    var endIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        declarationViewModel.loadDeclarations(context)
        userViewModel.loadDrivers(context)
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
    val list = declarations.filter { decl ->
        decl.routeId == routeId &&
        preferred.contains(runCatching { VehicleType.valueOf(decl.vehicleType) }.getOrNull()) &&
        startIndex >= 0 && endIndex >= 0 && startIndex < endIndex
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
                LazyColumn {
                    items(list) { decl ->
                        val driver = driverNames[decl.driverId] ?: ""
                        val type = runCatching { VehicleType.valueOf(decl.vehicleType) }.getOrNull()
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(driver, modifier = Modifier.weight(1f))
                            Text(type?.let { labelForVehicle(it) } ?: "", modifier = Modifier.weight(1f))
                            Text(decl.cost.toString(), modifier = Modifier.weight(1f))
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
