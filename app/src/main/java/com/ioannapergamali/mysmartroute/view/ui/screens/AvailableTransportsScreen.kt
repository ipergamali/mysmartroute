package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.view.ui.util.iconForVehicle
import com.ioannapergamali.mysmartroute.viewmodel.FavoritesViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.ReservationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.BookingViewModel
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.utils.matchesFavorites
import com.ioannapergamali.mysmartroute.utils.isUpcoming
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationDetailEntity
import kotlinx.coroutines.launch
import kotlin.math.max
import java.time.Instant
import java.time.ZoneId
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate

private val ColumnWidth = 120.dp


@Composable
private fun HeaderRow(scrollState: ScrollState) {
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.width(40.dp))
        Text(stringResource(R.string.driver), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.route_name), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.cost), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.date), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.time), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.seats_label), modifier = Modifier.width(ColumnWidth))
    }
    Divider()
}

@Composable
private fun DetailHeaderRow(scrollState: ScrollState) {
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp)
    ) {
        Text(stringResource(R.string.start_point), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.destination), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.route_name), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.seats_label), modifier = Modifier.width(ColumnWidth))
        Text(stringResource(R.string.reserve_seat), modifier = Modifier.width(ColumnWidth))
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
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val reservationViewModel: ReservationViewModel = viewModel()
    val bookingViewModel: BookingViewModel = viewModel()
    val poiViewModel: PoIViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val declarations by declarationViewModel.pendingDeclarations.collectAsState()
    val drivers by userViewModel.drivers.collectAsState()
    val pois by poiViewModel.pois.collectAsState()
    val routes by routeViewModel.routes.collectAsState()
    val preferred by favoritesViewModel.preferredFlow(context).collectAsState(initial = emptySet())
    val nonPreferred by favoritesViewModel.nonPreferredFlow(context).collectAsState(initial = emptySet())

    val reservationCounts = remember { mutableStateMapOf<String, Int>() }
    val detailsMap = remember { mutableStateMapOf<String, List<TransportDeclarationDetailEntity>>() }
    val detailReservationCounts = remember { mutableStateMapOf<String, MutableMap<String, Int>>() }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        declarationViewModel.loadDeclarations(context)
        userViewModel.loadDrivers(context)
        poiViewModel.loadPois(context)
        routeViewModel.loadRoutes(context, includeAll = true)
    }

    val driverNames = drivers.associate { it.id to "${it.name} ${it.surname}" }
    val poiNames = pois.associate { it.id to it.name }
    val routeNames = routes.associate { it.id to it.name }

    LaunchedEffect(declarations) {
        declarations.forEach { decl ->
            val count = reservationViewModel.getReservationCount(context, decl.id)
            reservationCounts[decl.id] = count
            if (detailsMap[decl.id] == null) {
                val details = declarationViewModel.fetchDetails(decl.id)
                detailsMap[decl.id] = details
                val counts = mutableMapOf<String, Int>()
                details.forEach { detail ->
                    val segCount = reservationViewModel.getReservationCountForSegment(
                        context,
                        decl.id,
                        detail.startPoiId,
                        detail.endPoiId
                    )
                    counts[detail.id] = segCount
                }
                detailReservationCounts[decl.id] = counts
            }
        }
    }

    val today = remember { LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val sortedDecls = declarations.filter { decl ->
        if (routeId != null && decl.routeId != routeId) return@filter false
        if (startId != null && endId != null) {
            val dets = detailsMap[decl.id] ?: emptyList()
            if (dets.none { it.startPoiId == startId && it.endPoiId == endId }) return@filter false
        }
        // Η δήλωση πρέπει να έχει κόστος μικρότερο ή ίσο με αυτό που όρισε ο χρήστης
        if (maxCost != null && decl.cost > maxCost) return@filter false
        val reserved = reservationCounts[decl.id] ?: 0
        val availableSeats = max(0, decl.seats - reserved)
        if (availableSeats <= 0) return@filter false
        if (decl.date < today) return@filter false
        if (date != null && date >= today && decl.date != date) return@filter false
        if (!decl.matchesFavorites(preferred, nonPreferred)) return@filter false
        if (!decl.isUpcoming()) return@filter false
        true
    }
        // ταξινόμηση βάσει κόστους ώστε οι φθηνότερες επιλογές να εμφανίζονται πρώτες
        .sortedBy { it.cost }


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
                    item { HeaderRow(scrollState) }
                    items(sortedDecls) { decl ->
                        val driver = driverNames[decl.driverId] ?: ""
                        val type = runCatching { VehicleType.valueOf(decl.vehicleType) }.getOrNull()
                        val preferredType = type != null && preferred.contains(type)

                        val routeName = routeNames[decl.routeId] ?: ""

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
                            Row(modifier = Modifier.horizontalScroll(scrollState)) {
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
                                Text(driver, modifier = Modifier.width(ColumnWidth))
                                Text(routeName, modifier = Modifier.width(ColumnWidth))
                                Text(decl.cost.toString(), modifier = Modifier.width(ColumnWidth))
                                Text(dateText, modifier = Modifier.width(ColumnWidth))
                                Text(timeText, modifier = Modifier.width(ColumnWidth))
                                Text(availableSeats.toString(), modifier = Modifier.width(ColumnWidth))
                            }
                            val dets = detailsMap[decl.id] ?: emptyList()
                            if (dets.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                DetailHeaderRow(scrollState)
                                dets.forEach { detail ->
                                    val startName = poiNames[detail.startPoiId] ?: detail.startPoiId
                                    val endName = poiNames[detail.endPoiId] ?: detail.endPoiId
                                    val reservedSeg = detailReservationCounts[decl.id]?.get(detail.id) ?: 0
                                    val availableSeg = max(0, detail.seats - reservedSeg)
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(scrollState)
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(startName, modifier = Modifier.width(ColumnWidth))
                                        Text(endName, modifier = Modifier.width(ColumnWidth))
                                        Text(routeName, modifier = Modifier.width(ColumnWidth))
                                        Text(availableSeg.toString(), modifier = Modifier.width(ColumnWidth))
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val result = bookingViewModel.reserveSeat(
                                                        context = context,
                                                        routeId = decl.routeId,
                                                        date = decl.date,
                                                        startTime = decl.startTime,
                                                        startPoiId = detail.startPoiId,
                                                        endPoiId = detail.endPoiId,
                                                        declarationId = decl.id,
                                                        driverId = decl.driverId,
                                                        vehicleId = detail.vehicleId,
                                                        cost = decl.cost,
                                                        durationMinutes = decl.durationMinutes
                                                    )
                                                    message = result.fold(
                                                        onSuccess = {
                                                            val map = detailReservationCounts.getOrPut(decl.id) { mutableMapOf() }
                                                            map[detail.id] = reservedSeg + 1
                                                            reservationCounts[decl.id] = (reservationCounts[decl.id] ?: 0) + 1
                                                            context.getString(R.string.seat_booked)
                                                        },
                                                        onFailure = { context.getString(R.string.seat_unavailable) }
                                                    )
                                                }
                                            },
                                            enabled = availableSeg > 0,
                                            modifier = Modifier.width(ColumnWidth)
                                        ) {
                                            Text(stringResource(R.string.reserve_seat))
                                        }
                                    }
                                }
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
