package com.ioannapergamali.mysmartroute.view.ui.screens

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.DatabaseData
import com.ioannapergamali.mysmartroute.data.local.FavoriteEntity
import com.ioannapergamali.mysmartroute.data.local.FavoriteRouteEntity
import com.ioannapergamali.mysmartroute.data.local.LanguageSettingEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.NotificationEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.data.local.TransferRequestEntity
import com.ioannapergamali.mysmartroute.data.local.TripRatingEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.UserPoiEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import com.ioannapergamali.mysmartroute.viewmodel.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DatabaseSyncScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val syncState by viewModel.syncState.collectAsState()
    val lastSync by viewModel.lastSyncTime.collectAsState()
    val localData by viewModel.localData.collectAsState()
    val firebaseData by viewModel.firebaseData.collectAsState()
    val currentMessage by viewModel.currentSyncMessage.collectAsState()
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableStateOf(SyncTab.LOCAL) }

    LaunchedEffect(Unit) {
        viewModel.loadLastSync(context)
        viewModel.loadLocalData(context)
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            SyncTab.LOCAL -> viewModel.loadLocalData(context)
            SyncTab.FIRESTORE -> viewModel.loadFirebaseData()
            SyncTab.SYNC -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.sync_db),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            SyncTabSelector(selectedTab = selectedTab) { selectedTab = it }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                SyncTab.LOCAL -> {
                    DatabaseDataTab(
                        title = stringResource(R.string.sync_local_title),
                        data = localData,
                        emptyMessage = stringResource(R.string.sync_empty_table),
                        refreshLabel = stringResource(R.string.sync_reload_local),
                        onRefresh = { viewModel.loadLocalData(context) }
                    )
                }

                SyncTab.FIRESTORE -> {
                    DatabaseDataTab(
                        title = stringResource(R.string.sync_firestore_title),
                        data = firebaseData,
                        emptyMessage = stringResource(R.string.sync_empty_table),
                        refreshLabel = stringResource(R.string.sync_reload_firestore),
                        onRefresh = { viewModel.loadFirebaseData() }
                    )
                }

                SyncTab.SYNC -> {
                    SyncActions(
                        syncState = syncState,
                        lastSync = lastSync,
                        currentMessage = currentMessage,
                        onSync = { viewModel.syncDatabases(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncTabSelector(selectedTab: SyncTab, onSelect: (SyncTab) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SyncTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index, SyncTab.entries.size)
            ) {
                Text(text = stringResource(id = tab.titleRes))
            }
        }
    }
}

@Composable
private fun DatabaseDataTab(
    title: String,
    data: DatabaseData?,
    emptyMessage: String,
    refreshLabel: String,
    onRefresh: () -> Unit
) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(onClick = onRefresh, enabled = data != null) {
        Text(refreshLabel)
    }
    Spacer(modifier = Modifier.height(16.dp))

    if (data == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val context = LocalContext.current
        val tables = remember(data, context) { data.toTableDisplays(context) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tables, key = { it.key }) { table ->
                DatabaseTableCard(table = table, emptyMessage = emptyMessage)
            }
        }
    }
}

@Composable
private fun DatabaseTableCard(table: TableDisplay, emptyMessage: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = table.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (table.rows.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                table.rows.forEachIndexed { index, row ->
                    Text(
                        text = row,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (index < table.rows.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncActions(
    syncState: SyncState,
    lastSync: Long,
    currentMessage: String?,
    onSync: () -> Unit
) {
    val lastSyncText = remember(lastSync) { formatTimestamp(lastSync) }

    Text(
        text = stringResource(R.string.sync_last_sync, lastSyncText),
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onSync,
        enabled = syncState !is SyncState.Loading
    ) {
        Text(text = stringResource(R.string.sync_start))
    }

    Spacer(modifier = Modifier.height(16.dp))

    when (syncState) {
        is SyncState.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentMessage ?: stringResource(R.string.sync_loading),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        is SyncState.Success -> {
            Text(
                text = stringResource(R.string.sync_success),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        is SyncState.Error -> {
            val message = (syncState as SyncState.Error).message
            Text(
                text = stringResource(R.string.sync_error_prefix, message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        SyncState.Idle -> Unit
    }
}

private enum class SyncTab(@StringRes val titleRes: Int) {
    LOCAL(R.string.sync_tab_local),
    FIRESTORE(R.string.sync_tab_firestore),
    SYNC(R.string.sync_tab_action)
}

private data class TableDisplay(
    val key: String,
    val title: String,
    val rows: List<String>
)

private fun DatabaseData.toTableDisplays(context: Context): List<TableDisplay> {
    val yesText = context.getString(R.string.common_yes)
    val noText = context.getString(R.string.common_no)

    fun Boolean.toLabel() = if (this) yesText else noText
    fun String.ifBlankDash(): String = ifBlank { "-" }

    val tables = mutableListOf<TableDisplay>()

    tables += TableDisplay(
        key = "users",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_users), users.size),
        rows = users.map { user: UserEntity ->
            context.getString(
                R.string.table_users_row,
                user.id.ifBlankDash(),
                user.name.ifBlankDash(),
                user.surname.ifBlankDash(),
                user.username.ifBlankDash(),
                user.role.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "vehicles",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_vehicles), vehicles.size),
        rows = vehicles.map { vehicle: VehicleEntity ->
            context.getString(
                R.string.table_vehicles_row,
                vehicle.id.ifBlankDash(),
                vehicle.name.ifBlankDash(),
                vehicle.description.ifBlankDash(),
                vehicle.type.ifBlankDash(),
                vehicle.seat,
                vehicle.userId.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "poi_types",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_poi_types), poiTypes.size),
        rows = poiTypes.map { type ->
            context.getString(R.string.table_poi_types_row, type.id.ifBlankDash(), type.name.ifBlankDash())
        }
    )

    tables += TableDisplay(
        key = "pois",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_pois), pois.size),
        rows = pois.map { poi: PoIEntity ->
            context.getString(
                R.string.table_pois_row,
                poi.id.ifBlankDash(),
                poi.name.ifBlankDash(),
                poi.address.city.ifBlankDash(),
                poi.address.streetName.ifBlankDash(),
                poi.address.streetNum
            )
        }
    )

    tables += TableDisplay(
        key = "settings",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_settings), settings.size),
        rows = settings.map { setting: SettingsEntity ->
            context.getString(
                R.string.table_settings_row,
                setting.userId.ifBlankDash(),
                setting.theme.ifBlankDash(),
                setting.darkTheme.toLabel(),
                setting.language.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "roles",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_roles), roles.size),
        rows = roles.map { role ->
            context.getString(
                R.string.table_roles_row,
                role.id.ifBlankDash(),
                role.name.ifBlankDash(),
                role.parentRoleId ?: "-"
            )
        }
    )

    tables += TableDisplay(
        key = "menus",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_menus), menus.size),
        rows = menus.map { menu: MenuEntity ->
            context.getString(
                R.string.table_menus_row,
                menu.id.ifBlankDash(),
                menu.roleId.ifBlankDash(),
                menu.titleResKey.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "menu_options",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_menu_options), menuOptions.size),
        rows = menuOptions.map { option: MenuOptionEntity ->
            context.getString(
                R.string.table_menu_options_row,
                option.id.ifBlankDash(),
                option.menuId.ifBlankDash(),
                option.titleResKey.ifBlankDash(),
                option.route.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "app_language",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_languages), languages.size),
        rows = languages.map { language: LanguageSettingEntity ->
            context.getString(R.string.table_languages_row, language.id, language.language.ifBlankDash())
        }
    )

    tables += TableDisplay(
        key = "routes",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_routes), routes.size),
        rows = routes.map { route: RouteEntity ->
            context.getString(
                R.string.table_routes_row,
                route.id.ifBlankDash(),
                route.name.ifBlankDash(),
                route.startPoiId.ifBlankDash(),
                route.endPoiId.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "route_points",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_route_points), routePoints.size),
        rows = routePoints.map { point: RoutePointEntity ->
            context.getString(
                R.string.table_route_points_row,
                point.routeId.ifBlankDash(),
                point.position,
                point.poiId.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "movings",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_movings), movings.size),
        rows = movings.map { moving: MovingEntity ->
            context.getString(
                R.string.table_movings_row,
                moving.id.ifBlankDash(),
                moving.routeId.ifBlankDash(),
                moving.userId.ifBlankDash(),
                moving.status.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "transport_declarations",
        title = context.getString(
            R.string.sync_table_title,
            context.getString(R.string.table_name_transport_declarations),
            declarations.size
        ),
        rows = declarations.map { declaration: TransportDeclarationEntity ->
            context.getString(
                R.string.table_transport_declarations_row,
                declaration.id.ifBlankDash(),
                declaration.routeId.ifBlankDash(),
                declaration.driverId.ifBlankDash(),
                declaration.vehicleType.ifBlankDash(),
                declaration.seats,
                declaration.cost,
                declaration.durationMinutes
            )
        }
    )

    tables += TableDisplay(
        key = "availabilities",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_availabilities), availabilities.size),
        rows = availabilities.map { availability ->
            context.getString(
                R.string.table_availabilities_row,
                availability.id.ifBlankDash(),
                availability.userId.ifBlankDash(),
                formatDate(availability.date),
                formatMinutes(availability.fromTime),
                formatMinutes(availability.toTime)
            )
        }
    )

    tables += TableDisplay(
        key = "favorites",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_favorites), favorites.size),
        rows = favorites.map { favorite: FavoriteEntity ->
            context.getString(
                R.string.table_favorites_row,
                favorite.id.ifBlankDash(),
                favorite.userId.ifBlankDash(),
                favorite.vehicleType.ifBlankDash(),
                favorite.preferred.toLabel()
            )
        }
    )

    tables += TableDisplay(
        key = "favorite_routes",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_favorite_routes), favoriteRoutes.size),
        rows = favoriteRoutes.map { route: FavoriteRouteEntity ->
            context.getString(
                R.string.table_favorite_routes_row,
                route.userId.ifBlankDash(),
                route.routeId.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "user_pois",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_user_pois), userPois.size),
        rows = userPois.map { userPoi: UserPoiEntity ->
            context.getString(
                R.string.table_user_pois_row,
                userPoi.id.ifBlankDash(),
                userPoi.userId.ifBlankDash(),
                userPoi.poiId.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "seat_reservations",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_seat_reservations), seatReservations.size),
        rows = seatReservations.map { reservation: SeatReservationEntity ->
            context.getString(
                R.string.table_seat_reservations_row,
                reservation.id.ifBlankDash(),
                reservation.routeId.ifBlankDash(),
                reservation.userId.ifBlankDash(),
                formatTimestamp(reservation.date, includeTime = true),
                formatTimeFromMillis(reservation.startTime)
            )
        }
    )

    tables += TableDisplay(
        key = "transfer_requests",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_transfer_requests), transferRequests.size),
        rows = transferRequests.map { request: TransferRequestEntity ->
            context.getString(
                R.string.table_transfer_requests_row,
                request.requestNumber,
                request.routeId.ifBlankDash(),
                request.passengerId.ifBlankDash(),
                request.driverId.ifBlankDash(),
                request.status.name
            )
        }
    )

    tables += TableDisplay(
        key = "trip_ratings",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_trip_ratings), tripRatings.size),
        rows = tripRatings.map { rating: TripRatingEntity ->
            context.getString(
                R.string.table_trip_ratings_row,
                rating.movingId.ifBlankDash(),
                rating.userId.ifBlankDash(),
                rating.rating,
                rating.comment.ifBlankDash()
            )
        }
    )

    tables += TableDisplay(
        key = "notifications",
        title = context.getString(R.string.sync_table_title, context.getString(R.string.table_name_notifications), notifications.size),
        rows = notifications.map { notification: NotificationEntity ->
            context.getString(
                R.string.table_notifications_row,
                notification.id.ifBlankDash(),
                notification.userId.ifBlankDash(),
                notification.message.ifBlankDash()
            )
        }
    )

    return tables
}

private fun formatTimestamp(timestamp: Long, includeTime: Boolean = false): String {
    if (timestamp == 0L) return "-"
    val pattern = if (includeTime) "dd/MM/yyyy HH:mm" else "dd/MM/yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

private fun formatDate(timestamp: Long): String = formatTimestamp(timestamp, includeTime = false)

private fun formatMinutes(value: Int): String {
    val hours = value / 60
    val minutes = value % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}

private fun formatTimeFromMillis(value: Long): String {
    val minutes = (value / 60000L).toInt()
    return formatMinutes(minutes)
}
