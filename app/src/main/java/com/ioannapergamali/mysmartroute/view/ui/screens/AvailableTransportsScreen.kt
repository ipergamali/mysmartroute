package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

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
import com.ioannapergamali.mysmartroute.viewmodel.ReservationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.BookingViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleViewModel
import com.ioannapergamali.mysmartroute.utils.matchesFavorites
import kotlinx.coroutines.launch
import kotlin.math.max
import java.time.Instant
import java.time.ZoneId
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.width(40.dp))
        Text(stringResource(R.string.driver), modifier = Modifier.weight(1f))
        Text(stringResource(R.string.vehicle_name), modifier = Modifier.weight(1f))
        Text(stringResource(R.string.vehicle_type), modifier = Modifier.weight(1f))
        Text(stringResource(R.string.cost), modifier = Modifier.weight(1f))
        Text(stringResource(R.string.seats_label), modifier = Modifier.weight(1f))
        Text(stringResource(R.string.date), modifier = Modifier.weight(1f))
        Text(stringResource(R.string.time), modifier = Modifier.weight(1f))
    }
    Divider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableTransportsScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    routeId: String?,
    startId: String?,
    endId: String?,
    maxCost: Double?,
    date: Long?
) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val reservationViewModel: ReservationViewModel = viewModel()
    val bookingViewModel: BookingViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val declarations by declarationViewModel.pendingDeclarations.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val preferred by favoritesViewModel.preferredFlow(context).collectAsState(initial = emptySet())
    val nonPreferred by favoritesViewModel.nonPreferredFlow(context).collectAsState(initial = emptySet())

    val reservationCounts = remember { mutableStateMapOf<String, Int>() }
    var message by remember { mutableStateOf("") }

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

    val driverNames = drivers.associate { it.id to "${it.name} ${it.surname}" }
    val vehiclesMap = vehicles.associateBy { it.id }
    val sortedDecls = declarations.filter { decl ->
        if (decl.routeId != routeId) return@filter false
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) return@filter false
        if (maxCost != null && decl.cost > maxCost) return@filter false
        if (date != null && date > 0 && decl.date != date) return@filter false
        if (!decl.matchesFavorites(preferred, nonPreferred)) return@filter false
        true
    }
        // ταξινόμηση βάσει κόστους ώστε οι φθηνότερες επιλογές να εμφανίζονται πρώτες
        .sortedBy { it.cost }

    LaunchedEffect(sortedDecls) {
        sortedDecls.forEach { decl ->
            val count = reservationViewModel.getReservationCount(context, decl.id)
            reservationCounts[decl.id] = count
        }
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
            if (sortedDecls.isEmpty()) {
                Text(stringResource(R.string.no_transports_found))
            } else {
                val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
                val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }


                LazyColumn {
                    item { HeaderRow() }
                    items(sortedDecls) { decl ->
                        val driver = driverNames[decl.driverId] ?: ""
                        val type = runCatching { VehicleType.valueOf(decl.vehicleType) }.getOrNull()
                        val preferredType = type != null && preferred.contains(type)

                        if (vehiclesMap[decl.vehicleId] == null) {
                            vehicleViewModel.loadVehicleById(context, decl.vehicleId)
                        }
                        val vehicleName = vehiclesMap[decl.vehicleId]?.name ?: ""

                        val dateText = Instant.ofEpochMilli(decl.date)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(formatter)
                        val timeText = LocalTime.ofSecondOfDay(decl.startTime / 1000)
                            .format(timeFormatter)

                        val reserved = reservationCounts[decl.id] ?: 0
                        val availableSeats = max(0, decl.seats - reserved)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (preferredType) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                }
                                type?.let {
                                    Icon(
                                        imageVector = iconForVehicle(it),
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
                                Text(driver, modifier = Modifier.weight(1f))
                                Text(vehicleName, modifier = Modifier.weight(1f))
                                Text(type?.let { labelForVehicle(it) } ?: "", modifier = Modifier.weight(1f))
                                Text(decl.cost.toString(), modifier = Modifier.weight(1f))
                                Text(availableSeats.toString(), modifier = Modifier.weight(1f))
                                Text(dateText, modifier = Modifier.weight(1f))
                                Text(timeText, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                          val result = bookingViewModel.reserveSeat(
                                              context = context,
                                              routeId = decl.routeId,
                                              date = decl.date,
                                              startTime = decl.startTime,
                                              startPoiId = startId ?: "",
                                              endPoiId = endId ?: "",
                                              declarationId = decl.id
                                          )
                                        message = result.fold(
                                            onSuccess = {
                                                reservationCounts[decl.id] = reserved + 1
                                                context.getString(R.string.seat_booked)
                                            },
                                            onFailure = {
                                                context.getString(R.string.seat_unavailable)
                                            }
                                        )
                                    }
                                },
                                enabled = startId != null && endId != null && availableSeats > 0
                            ) {
                                Text(stringResource(R.string.reserve_seat))
                            }
                        }
                        Divider()
                    }
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(message)
                }
            }
        }
    }
}
