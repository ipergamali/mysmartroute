package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.PoiTypeEntity
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.insertSettingsSafely
import com.ioannapergamali.mysmartroute.data.local.insertUserSafely
import com.ioannapergamali.mysmartroute.data.local.insertVehicleSafely
import com.ioannapergamali.mysmartroute.data.local.insertMenuSafely
import com.ioannapergamali.mysmartroute.data.local.RoleEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.utils.toPoIEntity
import com.ioannapergamali.mysmartroute.utils.toPoiTypeEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.utils.toVehicleEntity
import com.ioannapergamali.mysmartroute.data.local.LanguageSettingEntity
import com.ioannapergamali.mysmartroute.data.local.FavoriteEntity
import com.ioannapergamali.mysmartroute.data.local.insertFavoriteSafely
import com.ioannapergamali.mysmartroute.utils.toFavoriteEntity
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationDetailEntity
import com.ioannapergamali.mysmartroute.data.local.FavoriteRouteEntity
import com.ioannapergamali.mysmartroute.data.local.UserPoiEntity
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.RouteBusStationEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationDetailEntity
import com.ioannapergamali.mysmartroute.data.local.AvailabilityEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationDetailEntity
import com.ioannapergamali.mysmartroute.data.local.TransferRequestEntity
import com.ioannapergamali.mysmartroute.data.local.TripRatingEntity
import com.ioannapergamali.mysmartroute.data.local.NotificationEntity
import com.ioannapergamali.mysmartroute.utils.toRouteWithStations
import com.ioannapergamali.mysmartroute.utils.toMovingEntity
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationEntity
import com.ioannapergamali.mysmartroute.utils.toAvailabilityEntity
import com.ioannapergamali.mysmartroute.utils.toSeatReservationEntity
import com.ioannapergamali.mysmartroute.utils.toSeatReservationDetailEntity
import com.ioannapergamali.mysmartroute.utils.toTransferRequestEntity
import com.ioannapergamali.mysmartroute.utils.toTripRatingEntity
import com.ioannapergamali.mysmartroute.utils.toNotificationEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.data.local.MovingDetailEntity
import com.ioannapergamali.mysmartroute.utils.toMovingDetailEntity
import com.ioannapergamali.mysmartroute.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ViewModel για προβολή και συγχρονισμό δεδομένων βάσεων.
 * ViewModel for viewing and synchronizing database data.
 */
class DatabaseViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val favoritesGroup
        get() = firestore.collectionGroup("items")

    private fun userVehicles(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection("favorites")
        .document("vehicles")
        .collection("items")

    companion object {
        private const val TAG = "DatabaseViewModel"
    }

    private val _localData = MutableStateFlow<DatabaseData?>(null)
    val localData: StateFlow<DatabaseData?> = _localData

    private val _firebaseData = MutableStateFlow<DatabaseData?>(null)
    val firebaseData: StateFlow<DatabaseData?> = _firebaseData

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    private val localTableDefinitions = listOf(
        "users",
        "vehicles",
        "poi_types",
        "pois",
        "settings",
        "roles",
        "menus",
        "menu_options",
        "app_language",
        "routes",
        "route_points",
        "route_bus_station",
        "movings",
        "moving_details",
        "walking",
        "walking_routes",
        "transport_declarations",
        "transport_declarations_details",
        "availabilities",
        "seat_reservations",
        "seat_reservation_details",
        "favorites",
        "favorite_routes",
        "transfer_requests",
        "trip_ratings",
        "notifications",
        "user_pois",
        "app_datetime"
    )

    private val firebaseTableDefinitions = listOf(
        FirebaseTableDefinition(
            collection = "users",
            subcollections = listOf(
                SubcollectionDefinition(
                    name = "favorites",
                    children = listOf(SubcollectionDefinition(name = "items"))
                )
            )
        ),
        FirebaseTableDefinition(collection = "vehicles"),
        FirebaseTableDefinition(collection = "pois"),
        FirebaseTableDefinition(collection = "poi_types"),
        FirebaseTableDefinition(collection = "user_settings"),
        FirebaseTableDefinition(
            collection = "roles",
            subcollections = listOf(
                SubcollectionDefinition(
                    name = "menus",
                    children = listOf(SubcollectionDefinition(name = "options"))
                )
            )
        ),
        FirebaseTableDefinition(
            collection = "routes",
            subcollections = listOf(SubcollectionDefinition(name = "bus_stations"))
        ),
        FirebaseTableDefinition(
            collection = "movings",
            subcollections = listOf(SubcollectionDefinition(name = "details"))
        ),
        FirebaseTableDefinition(
            collection = "transport_declarations",
            subcollections = listOf(SubcollectionDefinition(name = "details"))
        ),
        FirebaseTableDefinition(collection = "availabilities"),
        FirebaseTableDefinition(
            collection = "seat_reservations",
            subcollections = listOf(SubcollectionDefinition(name = "details"))
        ),
        FirebaseTableDefinition(collection = "transfer_requests"),
        FirebaseTableDefinition(collection = "trip_ratings"),
        FirebaseTableDefinition(collection = "notifications")
    )

    private val firebaseDefinitionsById = firebaseTableDefinitions.associateBy { it.collection }

    private val _localTables = MutableStateFlow(localTableDefinitions.map { TableToggleState(id = it, title = it) })
    val localTables: StateFlow<List<TableToggleState>> = _localTables

    private val _firebaseTables = MutableStateFlow(firebaseTableDefinitions.map { TableToggleState(id = it.collection, title = it.collection) })
    val firebaseTables: StateFlow<List<TableToggleState>> = _firebaseTables

    private val _clearState = MutableStateFlow<ClearState>(ClearState.Idle)
    val clearState: StateFlow<ClearState> = _clearState

    /**
     * Φορτώνει τον χρόνο τελευταίου συγχρονισμού από τα SharedPreferences.
     * Loads the last sync timestamp from SharedPreferences.
     */
    fun loadLastSync(context: Context) {
        val prefs = context.getSharedPreferences("db_sync", Context.MODE_PRIVATE)
        _lastSyncTime.value = prefs.getLong("last_sync", 0L)
    }

    fun setLocalTableSelected(tableId: String, selected: Boolean) {
        _localTables.update { tables ->
            tables.map { table ->
                if (table.id == tableId) {
                    table.copy(selected = selected)
                } else {
                    table
                }
            }
        }
        resetClearStateIfPossible()
    }

    fun setFirebaseTableSelected(tableId: String, selected: Boolean) {
        _firebaseTables.update { tables ->
            tables.map { table ->
                if (table.id == tableId) {
                    table.copy(selected = selected)
                } else {
                    table
                }
            }
        }
        resetClearStateIfPossible()
    }

    fun clearSelectedTables(context: Context) {
        val localToClear = _localTables.value.filter { it.selected }.map { it.id }
        val firebaseToClear = _firebaseTables.value.filter { it.selected }.map { it.id }

        if (localToClear.isEmpty() && firebaseToClear.isEmpty()) {
            _clearState.value = ClearState.Error(context.getString(R.string.clear_error_no_selection))
            return
        }

        clearTables(
            context = context,
            localToClear = localToClear,
            firebaseToClear = firebaseToClear,
            successMessage = context.getString(R.string.clear_success)
        )
    }

    fun clearAllTables(context: Context) {
        clearTables(
            context = context,
            localToClear = localTableDefinitions,
            firebaseToClear = firebaseTableDefinitions.map { it.collection },
            successMessage = context.getString(R.string.initialize_success)
        )
    }

    private fun clearTables(
        context: Context,
        localToClear: List<String>,
        firebaseToClear: List<String>,
        successMessage: String
    ) {
        viewModelScope.launch {
            _clearState.value = ClearState.Running
            try {
                clearLocalTables(context, localToClear)
                clearFirebaseCollections(firebaseToClear)
                _localTables.update { tables -> tables.map { it.copy(selected = false) } }
                _firebaseTables.update { tables -> tables.map { it.copy(selected = false) } }
                _clearState.value = ClearState.Success(successMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing tables", e)
                val reason = e.localizedMessage ?: e.message ?: ""
                val message = context.getString(R.string.clear_error_generic, reason.ifEmpty { "-" })
                _clearState.value = ClearState.Error(message)
            }
        }
    }

    private fun resetClearStateIfPossible() {
        val current = _clearState.value
        if (current is ClearState.Success || current is ClearState.Error) {
            _clearState.value = ClearState.Idle
        }
    }

    private suspend fun clearLocalTables(context: Context, tableNames: List<String>) {
        if (tableNames.isEmpty()) return
        withContext(Dispatchers.IO) {
            val db = MySmartRouteDatabase.getInstance(context)
            db.withTransaction {
                val sqliteDb = db.openHelper.writableDatabase
                tableNames.forEach { table ->
                    Log.d(TAG, "Clearing local table $table")
                    sqliteDb.execSQL("DELETE FROM `$table`")
                }
            }
        }
    }

    private suspend fun clearFirebaseCollections(tableNames: List<String>) {
        if (tableNames.isEmpty()) return
        withContext(Dispatchers.IO) {
            tableNames.forEach { name ->
                val definition = firebaseDefinitionsById[name] ?: FirebaseTableDefinition(collection = name)
                Log.d(TAG, "Clearing Firebase collection ${definition.collection}")
                clearFirebaseCollection(definition)
            }
        }
    }

    private suspend fun clearFirebaseCollection(definition: FirebaseTableDefinition) {
        val snapshot = firestore.collection(definition.collection).get().await()
        for (doc in snapshot.documents) {
            deleteSubcollections(doc.reference, definition.subcollections)
            doc.reference.delete().await()
        }
    }

    private suspend fun deleteSubcollections(
        document: DocumentReference,
        subcollections: List<SubcollectionDefinition>
    ) {
        if (subcollections.isEmpty()) return
        subcollections.forEach { subcollection ->
            val subSnapshot = document.collection(subcollection.name).get().await()
            for (doc in subSnapshot.documents) {
                deleteSubcollections(doc.reference, subcollection.children)
                doc.reference.delete().await()
            }
        }
    }

    /**
     * Συλλέγει όλα τα δεδομένα από την τοπική βάση και τα εκθέτει ως ροές.
     * Collects all data from the local database and exposes them as flows.
     */
    fun loadLocalData(context: Context) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            Log.d(TAG, "Loading local data")
            kotlinx.coroutines.flow.combine(
                db.userDao().getAllUsers(),
                db.vehicleDao().getVehicles(),
                db.poIDao().getAll(),
                db.poiTypeDao().getAll(),
                db.settingsDao().getAllSettings(),
                db.roleDao().getAllRoles(),
                db.menuDao().getAllMenus(),
                db.menuOptionDao().getAllMenuOptions(),
                db.languageSettingDao().getAll(),
                db.routeDao().getAll(),
                db.routePointDao().getAll(),
                db.movingDao().getAll(),
                db.transportDeclarationDao().getAll(),
                db.availabilityDao().getAll(),
                db.favoriteDao().getAll(),
                db.favoriteRouteDao().getAll(),
                db.userPoiDao().getAll(),
                db.seatReservationDao().getAll(),
                db.transferRequestDao().getAll(),
                db.tripRatingDao().getAll(),
                db.notificationDao().getAll()
            ) { values ->
                val users = values[0] as List<UserEntity>
                val vehicles = values[1] as List<VehicleEntity>
                val pois = values[2] as List<PoIEntity>
                val settings = values[4] as List<SettingsEntity>
                val roles = values[5] as List<RoleEntity>
                val menus = values[6] as List<MenuEntity>
                val options = values[7] as List<MenuOptionEntity>
                val languages = values[8] as List<LanguageSettingEntity>
                val poiTypes = values[3] as List<PoiTypeEntity>
                val routes = values[9] as List<RouteEntity>
                val routePoints = values[10] as List<RoutePointEntity>
                val movings = values[11] as List<MovingEntity>
                val declarations = values[12] as List<TransportDeclarationEntity>
                val availabilities = values[13] as List<AvailabilityEntity>
                val favorites = values[14] as List<FavoriteEntity>
                val favoriteRoutes = values[15] as List<FavoriteRouteEntity>
                val userPois = values[16] as List<UserPoiEntity>
                val seatReservations = values[17] as List<SeatReservationEntity>
                val transferRequests = values[18] as List<TransferRequestEntity>
                val tripRatings = values[19] as List<TripRatingEntity>
                val notifications = values[20] as List<NotificationEntity>

                DatabaseData(
                    users,
                    vehicles,
                    pois,
                    poiTypes,
                    settings,
                    roles,
                    menus,
                    options,
                    languages,
                    routes,
                    routePoints,
                    movings,
                    declarations,
                    availabilities,
                    favorites,
                    favoriteRoutes,
                    userPois,
                    seatReservations,
                    transferRequests,
                    tripRatings,
                    notifications
                )
            }.collect { data ->
                Log.d(
                    TAG,
                    "Local data -> users:${data.users.size} vehicles:${data.vehicles.size} " +
                    "pois:${data.pois.size} poiTypes:${data.poiTypes.size} settings:${data.settings.size} roles:${data.roles.size} " +
                    "menus:${data.menus.size} options:${data.menuOptions.size} routes:${data.routes.size} " +
                    "points:${data.routePoints.size} movings:${data.movings.size} declarations:${data.declarations.size}" +
                    " availabilities:${data.availabilities.size} favorites:${data.favorites.size} favRoutes:${data.favoriteRoutes.size} " +
                    "userPois:${data.userPois.size} seatRes:${data.seatReservations.size} transferReq:${data.transferRequests.size} " +
                    "tripRatings:${data.tripRatings.size} notifications:${data.notifications.size}"
                )
                _localData.value = data
            }
        }
    }

    /**
     * Αντλεί δεδομένα από το Firebase Firestore για πλήρη εικόνα του συστήματος.
     * Retrieves data from Firebase Firestore for a complete system view.
     */
    fun loadFirebaseData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading Firebase data")
                val users = firestore.collection("users").get().await()
                    .documents.mapNotNull { it.toUserEntity() }
                Log.d(TAG, "Fetched ${users.size} users from Firebase")
                val vehicles = firestore.collection("vehicles").get().await()
                    .documents.mapNotNull { doc -> doc.toVehicleEntity() }
                Log.d(TAG, "Fetched ${vehicles.size} vehicles from Firebase")
                val pois = firestore.collection("pois").get().await()
                    .documents.mapNotNull { it.toPoIEntity() }
                Log.d(TAG, "Fetched ${pois.size} pois from Firebase")
                val poiTypes = firestore.collection("poi_types").get().await()
                    .documents.mapNotNull { doc: com.google.firebase.firestore.DocumentSnapshot ->
                        doc.toPoiTypeEntity()
                    }
                Log.d(TAG, "Fetched ${poiTypes.size} poi types from Firebase")
                val settings = firestore.collection("user_settings").get().await()
                    .documents.mapNotNull { doc ->
                        val userId = when (val uid = doc.get("userId")) {
                            is String -> uid
                            is DocumentReference -> uid.id
                            else -> null
                        } ?: return@mapNotNull null
                        SettingsEntity(
                            userId = userId,
                            theme = doc.getString("theme") ?: "",
                            darkTheme = doc.getBoolean("darkTheme") ?: false,
                            font = doc.getString("font") ?: "",
                            soundEnabled = doc.getBoolean("soundEnabled") ?: false,
                            soundVolume = (doc.getDouble("soundVolume") ?: 0.0).toFloat(),
                            language = doc.getString("language") ?: "el"
                        )
                    }
                Log.d(TAG, "Fetched ${settings.size} settings from Firebase")

                Log.d(TAG, "Fetching roles from Firestore")
                val rolesSnap = firestore.collection("roles").get().await()
                Log.d(TAG, "Fetched ${rolesSnap.documents.size} role documents")
                val roles = rolesSnap.documents.map { doc ->
                    RoleEntity(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        parentRoleId = doc.getString("parentRoleId")?.takeIf { it.isNotEmpty() }
                    )
                }
                Log.d(TAG, "Fetched ${roles.size} roles from Firebase")
                val menus = mutableListOf<MenuEntity>()
                val menuOptions = mutableListOf<MenuOptionEntity>()
                for (roleDoc in rolesSnap.documents) {
                    val roleId = roleDoc.getString("id") ?: roleDoc.id
                    Log.d(TAG, "Fetching menus for role $roleId")
                    val menusSnap = roleDoc.reference.collection("menus").get().await()
                    Log.d(TAG, "Fetched ${menusSnap.documents.size} menus for role $roleId")
                    for (menuDoc in menusSnap.documents) {
                        val menuId = menuDoc.getString("id") ?: menuDoc.id
                        val menuTitleKey = menuDoc.getString("titleKey")
                            ?: menuDoc.getString("titleResKey")
                            ?: ""
                        menus.add(
                            MenuEntity(
                                id = menuId,
                                roleId = roleId,
                                titleResKey = menuTitleKey
                            )
                        )
                        Log.d(TAG, "Fetching options for menu $menuId")
                        val optsSnap = menuDoc.reference.collection("options").get().await()
                        Log.d(TAG, "Fetched ${optsSnap.documents.size} options for menu $menuId")
                        for (optDoc in optsSnap.documents) {
                            val optionTitleKey = optDoc.getString("titleKey") ?: optDoc.getString("titleResKey") ?: ""
                            menuOptions.add(
                                MenuOptionEntity(
                                    id = optDoc.getString("id") ?: optDoc.id,
                                    menuId = menuId,
                                    titleResKey = optionTitleKey,
                                    route = optDoc.getString("route") ?: ""
                                )
                            )
                        }
                    }
                }

            val routeTriples = firestore.collection("routes").get().await()
                .documents.mapNotNull { it.toRouteWithStations() }
            val routes = routeTriples.map { it.first }
            val routePoints = routeTriples.flatMap { it.second }
            val busStations = routeTriples.flatMap { it.third }

            val movings = firestore.collection("movings").get().await()
                .documents.mapNotNull { it.toMovingEntity() }

            val declSnap = firestore.collection("transport_declarations").get().await()
            val declarations = mutableListOf<TransportDeclarationEntity>()
            for (doc in declSnap.documents) {
                val decl = doc.toTransportDeclarationEntity() ?: continue
                val detailDoc = doc.reference.collection("details").get().await()
                    .documents.firstOrNull()?.toTransportDeclarationDetailEntity(decl.id)
                if (detailDoc != null) {
                    decl.vehicleId = detailDoc.vehicleId
                    decl.vehicleType = detailDoc.vehicleType
                    decl.seats = detailDoc.seats
                }
                declarations += decl
            }

            val availabilities = firestore.collection("availabilities").get().await()
                .documents.mapNotNull { it.toAvailabilityEntity() }

            val favorites = favoritesGroup
                .get()
                .await()
                .documents.mapNotNull { it.toFavoriteEntity() }
            val seatReservations = firestore.collection("seat_reservations").get().await()
                .documents.mapNotNull { it.toSeatReservationEntity() }

            val transferRequests = firestore.collection("transfer_requests").get().await()
                .documents.mapNotNull { it.toTransferRequestEntity() }

            val tripRatings = firestore.collection("trip_ratings").get().await()
                .documents.mapNotNull { it.toTripRatingEntity() }

            val notifications = firestore.collection("notifications").get().await()
                .documents.mapNotNull { it.toNotificationEntity() }

            val userPois = emptyList<UserPoiEntity>()

            Log.d(
                TAG,
                "Firebase data -> users:${users.size} vehicles:${vehicles.size} pois:${pois.size} types:${poiTypes.size} settings:${settings.size} roles:${roles.size} menus:${menus.size} options:${menuOptions.size} routes:${routes.size} movings:${movings.size} declarations:${declarations.size} availabilities:${availabilities.size} favorites:${favorites.size} seatRes:${seatReservations.size} transferReq:${transferRequests.size} tripRatings:${tripRatings.size}"
            )
            _firebaseData.value = DatabaseData(
                users,
                vehicles,
                pois,
                poiTypes,
                settings,
                roles,
                menus,
                menuOptions,
                emptyList(),
                routes,
                routePoints,
                movings,
                declarations,
                availabilities,
                favorites,
                emptyList(),
                userPois,
                seatReservations,
                transferRequests,
                tripRatings,
                notifications
            )
            } catch (e: FirebaseFirestoreException) {
                Log.e(TAG, "Firestore permission error", e)
                _syncState.value = SyncState.Error("Ανεπαρκή δικαιώματα στο Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Firebase data", e)
                _syncState.value = SyncState.Error("Σφάλμα φόρτωσης δεδομένων")
            }
        }
    }

    /**
     * Συγχρονίζει την τοπική βάση με το Firebase, ενημερώνοντας και τις δύο πλευρές.
     * Synchronizes the local database with Firebase, updating both sides.
     */
    fun syncDatabases(context: Context) {
        viewModelScope.launch {
            syncDatabasesSuspend(context)
        }
    }

    suspend fun syncDatabasesSuspend(context: Context) {
        Log.d(TAG, "Starting database synchronization")
        if (!NetworkUtils.isInternetAvailable(context)) {
            Log.w(TAG, "No internet connection")
            _syncState.value = SyncState.Error("No internet connection")
            return
        }
        Log.d(TAG, "Internet connection available")
        _syncState.value = SyncState.Loading
        val prefs = context.getSharedPreferences("db_sync", Context.MODE_PRIVATE)
        val localTs = prefs.getLong("last_sync", 0L)
        Log.d(TAG, "Local timestamp: $localTs")
        val remoteTs = try {
            firestore.collection("metadata").document("sync").get().await()
                .getLong("last_sync")?.also { Log.d(TAG, "Remote timestamp: $it") } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote timestamp", e)
            0L
        }

        Log.d(TAG, "Start sync: localTs=$localTs remoteTs=$remoteTs")

        val db = MySmartRouteDatabase.getInstance(context)

        try {
            withTimeout(30000L) {
                if (remoteTs > localTs) {
                    Log.d(TAG, "Remote database is newer, downloading data")
                    Log.d(TAG, "Fetching users from Firestore")
                    val users = firestore.collection("users").get().await()
                        .documents.mapNotNull { it.toUserEntity() }
                    Log.d(TAG, "Fetched ${users.size} users")
                    Log.d(TAG, "Fetching vehicles from Firestore")
                    val vehicles = firestore.collection("vehicles").get().await()
                        .documents.mapNotNull { doc -> doc.toVehicleEntity() }
                    Log.d(TAG, "Fetching PoIs from Firestore")
                    val pois = firestore.collection("pois").get().await()
                        .documents.mapNotNull { it.toPoIEntity() }
                    Log.d(TAG, "Fetched ${pois.size} pois")
                    Log.d(TAG, "Fetching PoiTypes from Firestore")
                    val poiTypes = firestore.collection("poi_types").get().await()
                        .documents.mapNotNull { doc: com.google.firebase.firestore.DocumentSnapshot ->
                            doc.toPoiTypeEntity()
                        }
                    Log.d(TAG, "Fetched ${poiTypes.size} poi types")
                    Log.d(TAG, "Fetching settings from Firestore")
                    val settings = firestore.collection("user_settings").get().await()
                        .documents.mapNotNull { doc ->
                            val userId = when (val uid = doc.get("userId")) {
                                is String -> uid
                                is DocumentReference -> uid.id
                                else -> null
                            } ?: return@mapNotNull null
                            SettingsEntity(
                                userId = userId,
                                theme = doc.getString("theme") ?: "",
                                darkTheme = doc.getBoolean("darkTheme") ?: false,
                                font = doc.getString("font") ?: "",
                                soundEnabled = doc.getBoolean("soundEnabled") ?: false,
                                soundVolume = (doc.getDouble("soundVolume") ?: 0.0).toFloat(),
                                language = doc.getString("language") ?: "el"
                            )
                        }

            Log.d(TAG, "Fetching roles from Firestore")
                    val rolesSnap = firestore.collection("roles").get().await()
            Log.d(TAG, "Fetched ${rolesSnap.documents.size} role documents")
                    val roles = rolesSnap.documents.map { doc ->
                        RoleEntity(
                            id = doc.getString("id") ?: doc.id,
                            name = doc.getString("name") ?: "",
                            parentRoleId = doc.getString("parentRoleId")?.takeIf { it.isNotEmpty() }
                        )
                    }
                    val menus = mutableListOf<MenuEntity>()
                    val menuOptions = mutableListOf<MenuOptionEntity>()
                    for (roleDoc in rolesSnap.documents) {
                        val roleId = roleDoc.getString("id") ?: roleDoc.id
                Log.d(TAG, "Fetching menus for role $roleId")
                        val menusSnap = roleDoc.reference.collection("menus").get().await()
                Log.d(TAG, "Fetched ${menusSnap.documents.size} menus for role $roleId")
                        for (menuDoc in menusSnap.documents) {
                            val menuId = menuDoc.getString("id") ?: menuDoc.id
                            menus.add(
                                MenuEntity(
                                    id = menuId,
                                    roleId = roleId,
                                    titleResKey = menuDoc.getString("titleKey") ?: ""
                                )
                            )
                    Log.d(TAG, "Fetching options for menu $menuId")
                            val optsSnap = menuDoc.reference.collection("options").get().await()
                    Log.d(TAG, "Fetched ${optsSnap.documents.size} options for menu $menuId")
                            for (optDoc in optsSnap.documents) {
                                menuOptions.add(
                                    MenuOptionEntity(
                                        id = optDoc.getString("id") ?: optDoc.id,
                                        menuId = menuId,
                                        titleResKey = optDoc.getString("titleKey") ?: "",
                                        route = optDoc.getString("route") ?: ""
                                    )
                                )
                            }
                        }
                    }

                    val routeTriples = firestore.collection("routes").get().await()
                        .documents.mapNotNull { it.toRouteWithStations() }
                    val routes = routeTriples.map { it.first }
                    val routePoints = routeTriples.flatMap { it.second }
                    val busStations = routeTriples.flatMap { it.third }

                    val movingSnap = firestore.collection("movings").get().await()
                    val movingDetails = mutableListOf<MovingDetailEntity>()
                    val movings = movingSnap.documents.mapNotNull { doc ->
                        val moving = doc.toMovingEntity()
                        if (moving != null) {
                            val dets = doc.reference.collection("details").get().await()
                            movingDetails += dets.documents.mapNotNull { it.toMovingDetailEntity(moving.id) }
                        }
                        moving
                    }

                    val declSnap = firestore.collection("transport_declarations").get().await()
                    val declarations = mutableListOf<TransportDeclarationEntity>()
                    val declDetails = mutableListOf<TransportDeclarationDetailEntity>()
                    for (doc in declSnap.documents) {
                        val decl = doc.toTransportDeclarationEntity() ?: continue
                        val details = doc.reference.collection("details").get().await()
                            .documents.mapNotNull { it.toTransportDeclarationDetailEntity(decl.id) }
                        if (details.isNotEmpty()) {
                            val first = details.first()
                            decl.vehicleId = first.vehicleId
                            decl.vehicleType = first.vehicleType
                            decl.seats = first.seats
                            declDetails.addAll(details)
                        }
                        declarations += decl
                    }

                    val availabilities = firestore.collection("availabilities").get().await()
                        .documents.mapNotNull { it.toAvailabilityEntity() }

                    val favorites = favoritesGroup
                        .get()
                        .await()
                        .documents.mapNotNull { it.toFavoriteEntity() }

                    val seatResSnap = firestore.collection("seat_reservations").get().await()
                    val seatResDetails = mutableListOf<SeatReservationDetailEntity>()
                    val seatReservations = seatResSnap.documents.mapNotNull { doc ->
                        val res = doc.toSeatReservationEntity()
                        if (res != null) {
                            val dets = doc.reference.collection("details").get().await()
                            seatResDetails += dets.documents.mapNotNull { it.toSeatReservationDetailEntity(res.id) }
                        }
                        res
                    }

                    val transferRequests = firestore.collection("transfer_requests").get().await()
                        .documents.mapNotNull { it.toTransferRequestEntity() }

                    val tripRatings = firestore.collection("trip_ratings").get().await()
                        .documents.mapNotNull { it.toTripRatingEntity() }

                    val userPois = emptyList<UserPoiEntity>()

                    Log.d(
                        TAG,
                        "Remote data -> users:${users.size} vehicles:${vehicles.size} pois:${pois.size} poiTypes:${poiTypes.size} settings:${settings.size} roles:${roles.size} menus:${menus.size} options:${menuOptions.size} routes:${routes.size} movings:${movings.size} declarations:${declarations.size} availabilities:${availabilities.size} favorites:${favorites.size} seatRes:${seatReservations.size} transferReq:${transferRequests.size} tripRatings:${tripRatings.size}"
                    )
                    users.forEach { insertUserSafely(db.userDao(), it) }
                    vehicles.forEach { insertVehicleSafely(db, it) }
                    pois.forEach { db.poIDao().insert(it) }
                    db.poiTypeDao().insertAll(poiTypes)
                    settings.forEach { insertSettingsSafely(db.settingsDao(), db.userDao(), it) }
                    roles.forEach { db.roleDao().insert(it) }
                    menus.forEach { insertMenuSafely(db.menuDao(), db.roleDao(), it) }
                    menuOptions.forEach { db.menuOptionDao().insert(it) }
                    routes.forEach { db.routeDao().insert(it) }
                    routePoints.forEach { db.routePointDao().insert(it) }
                    busStations.forEach { db.routeBusStationDao().insert(it) }
                    movings.forEach { db.movingDao().insert(it) }
                    movingDetails.forEach { db.movingDetailDao().insert(it) }
                    declarations.forEach { db.transportDeclarationDao().insert(it) }
                    if (declDetails.isNotEmpty()) {
                        db.transportDeclarationDetailDao().insertAll(declDetails)
                    }
                    availabilities.forEach { db.availabilityDao().insert(it) }
                    favorites.forEach { insertFavoriteSafely(db.favoriteDao(), db.userDao(), it) }
                    seatReservations.forEach { db.seatReservationDao().insert(it) }
                    seatResDetails.forEach { db.seatReservationDetailDao().insert(it) }
                    transferRequests.forEach { db.transferRequestDao().insert(it) }
                    tripRatings.forEach { db.tripRatingDao().upsert(it) }
                    Log.d(TAG, "Inserted remote data to local DB")
                    prefs.edit().putLong("last_sync", remoteTs).apply()
                    _lastSyncTime.value = remoteTs
                } else {
                    Log.d(TAG, "Local database is newer, uploading data")
                    val users = db.userDao().getAllUsers().first()
                    Log.d(TAG, "Fetched ${users.size} local users")
                    val vehicles = db.vehicleDao().getVehicles().first()
                    Log.d(TAG, "Fetched ${vehicles.size} local vehicles")
                    val pois = db.poIDao().getAll().first()
                    Log.d(TAG, "Fetched ${pois.size} local pois")
                    val poiTypes = db.poiTypeDao().getAll().first()
                    Log.d(TAG, "Fetched ${poiTypes.size} local poi types")
                    val settings = db.settingsDao().getAllSettings().first()
                    Log.d(TAG, "Fetched ${settings.size} local settings")
                    val roles = db.roleDao().getAllRoles().first()
                    Log.d(TAG, "Fetched ${roles.size} local roles")
                    val menus = db.menuDao().getAllMenus().first()
                    Log.d(TAG, "Fetched ${menus.size} local menus")
                    val menuOptions = db.menuOptionDao().getAllMenuOptions().first()
                    Log.d(TAG, "Fetched ${menuOptions.size} local options")
                    val routes = db.routeDao().getAll().first()
                    Log.d(TAG, "Fetched ${routes.size} local routes")
                    val routePoints = db.routePointDao().getAll().first()
                    Log.d(TAG, "Fetched ${routePoints.size} local points")
                    val movings = db.movingDao().getAll().first()
                    Log.d(TAG, "Fetched ${movings.size} local movings")
                    val declarations = db.transportDeclarationDao().getAll().first()
                    Log.d(TAG, "Fetched ${declarations.size} local declarations")
                    val detailDao = db.transportDeclarationDetailDao()
                    val movingDetailDao = db.movingDetailDao()
                    val seatResDetailDao = db.seatReservationDetailDao()

                    val availabilities = db.availabilityDao().getAll().first()
                    Log.d(TAG, "Fetched ${availabilities.size} local availabilities")
                    val favorites = db.favoriteDao().getAll().first()
                    Log.d(TAG, "Fetched ${favorites.size} local favorites")

                    val userPois = db.userPoiDao().getAll().first()
                    Log.d(TAG, "Fetched ${userPois.size} local user pois")

                    val seatReservations = db.seatReservationDao().getAll().first()
                    Log.d(TAG, "Fetched ${seatReservations.size} local seat reservations")

                    val transferRequests = db.transferRequestDao().getAll().first()
                    Log.d(TAG, "Fetched ${transferRequests.size} local transfer requests")

                    val tripRatings = db.tripRatingDao().getAll().first()
                    Log.d(TAG, "Fetched ${tripRatings.size} local trip ratings")

                    val notifications = db.notificationDao().getAll().first()
                    Log.d(TAG, "Fetched ${notifications.size} local notifications")

                    Log.d(
                        TAG,
                        "Local data -> users:${users.size} vehicles:${vehicles.size} pois:${pois.size} poiTypes:${poiTypes.size} settings:${settings.size} roles:${roles.size} menus:${menus.size} options:${menuOptions.size} routes:${routes.size} points:${routePoints.size} movings:${movings.size} declarations:${declarations.size} availabilities:${availabilities.size} favorites:${favorites.size} userPois:${userPois.size} seatRes:${seatReservations.size} transferReq:${transferRequests.size} tripRatings:${tripRatings.size} notifications:${notifications.size}"
                    )

                    // Αποφεύγουμε την δημιουργία κενών εγγράφων στο Firestore
                    // Avoid creating empty documents in Firestore
                    users.filter { it.id.isNotBlank() && it.name.isNotBlank() }.forEach { user ->
                        firestore.collection("users")
                            .document(user.id)
                            .set(user.toFirestoreMap()).await()
                    }
                    vehicles.forEach {
                        firestore.collection("vehicles")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    pois.forEach { firestore.collection("pois").document(it.id).set(it.toFirestoreMap()).await() }
                    poiTypes.forEach {
                        firestore.collection("poi_types")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    settings.forEach {
                        firestore.collection("user_settings")
                            .document(it.userId)
                            .set(it.toFirestoreMap()).await()
                    }
                    roles.forEach {
                        firestore.collection("roles")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    menus.forEach { menu ->
                        val ref = firestore.collection("roles")
                            .document(menu.roleId)
                            .collection("menus")
                            .document(menu.id)
                        ref.set(menu.toFirestoreMap()).await()
                        menuOptions.filter { it.menuId == menu.id }.forEach { opt ->
                            ref.collection("options")
                                .document(opt.id)
                                .set(opt.toFirestoreMap()).await()
                        }
                    }

                    routes.forEach { route ->
                        val points = routePoints.filter { it.routeId == route.id }
                        firestore.collection("routes")
                            .document(route.id)
                            .set(route.toFirestoreMap(points)).await()
                    }

                    movings.forEach { moving ->
                        val ref = firestore.collection("movings").document(moving.id)
                        ref.set(moving.toFirestoreMap()).await()
                        movingDetailDao.getForMoving(moving.id).first().forEach { detail ->
                            ref.collection("details")
                                .document(detail.id)
                                .set(detail.toFirestoreMap()).await()
                        }
                    }

                    declarations.forEach { decl ->
                        firestore.collection("transport_declarations")
                            .document(decl.id)
                            .set(decl.toFirestoreMap()).await()
                        val details = detailDao.getForDeclaration(decl.id)
                        details.forEach { detail ->
                            firestore.collection("transport_declarations")
                                .document(decl.id)
                                .collection("details")
                                .document(detail.id)
                                .set(detail.toFirestoreMap()).await()
                        }
                    }

                    availabilities.forEach {
                        firestore.collection("availabilities")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    favorites.forEach { fav ->
                        userVehicles(fav.userId)
                            .document(fav.id)
                            .set(fav.toFirestoreMap()).await()
                    }

                    seatReservations.forEach { res ->
                        val ref = firestore.collection("seat_reservations").document(res.id)
                        ref.set(res.toFirestoreMap()).await()
                        seatResDetailDao.getForReservation(res.id).first().forEach { detail ->
                            ref.collection("details")
                                .document(detail.id)
                                .set(detail.toFirestoreMap()).await()
                        }
                    }

                    transferRequests.forEach {
                        firestore.collection("transfer_requests")
                            .document(it.requestNumber.toString())
                            .set(it.toFirestoreMap()).await()
                    }

                    tripRatings.forEach {
                        firestore.collection("trip_ratings")
                            .document("${'$'}{it.movingId}_${'$'}{it.userId}")
                            .set(it.toFirestoreMap()).await()
                    }

                    notifications.forEach {
                        firestore.collection("notifications")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }

                    Log.d(TAG, "Uploaded local data to Firebase")

                    val newTs = System.currentTimeMillis()
                    firestore.collection("metadata").document("sync").set(mapOf("last_sync" to newTs)).await()
                    prefs.edit().putLong("last_sync", newTs).apply()
                    _lastSyncTime.value = newTs
                }
                }
                _syncState.value = SyncState.Success
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Sync timeout", e)
                _syncState.value = SyncState.Error("Sync timeout")
            } catch (e: Exception) {
                Log.e(TAG, "Sync error", e)
                _syncState.value = SyncState.Error(
                    e.localizedMessage ?: "Sync failed"
                )
            }
        }
    }

data class TableToggleState(
    val id: String,
    val title: String,
    val selected: Boolean = false
)

data class FirebaseTableDefinition(
    val collection: String,
    val subcollections: List<SubcollectionDefinition> = emptyList()
)

data class SubcollectionDefinition(
    val name: String,
    val children: List<SubcollectionDefinition> = emptyList()
)

sealed class ClearState {
    object Idle : ClearState()
    object Running : ClearState()
    data class Success(val message: String) : ClearState()
    data class Error(val message: String) : ClearState()
}

/** Δομή για τα δεδομένα κάθε βάσης. */
data class DatabaseData(
    val users: List<UserEntity>,
    val vehicles: List<VehicleEntity>,
    val pois: List<PoIEntity>,
    val poiTypes: List<PoiTypeEntity>,
    val settings: List<SettingsEntity>,
    val roles: List<RoleEntity>,
    val menus: List<MenuEntity>,
    val menuOptions: List<MenuOptionEntity>,
    val languages: List<LanguageSettingEntity>,
    val routes: List<RouteEntity>,
    val routePoints: List<RoutePointEntity>,
    val movings: List<MovingEntity>,
    val declarations: List<TransportDeclarationEntity>,
    val availabilities: List<AvailabilityEntity>,
    val favorites: List<FavoriteEntity>,
    val favoriteRoutes: List<FavoriteRouteEntity>,
    val userPois: List<UserPoiEntity>,
    val seatReservations: List<SeatReservationEntity>,
    val transferRequests: List<TransferRequestEntity>,
    val tripRatings: List<TripRatingEntity>,
    val notifications: List<NotificationEntity>
)

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
