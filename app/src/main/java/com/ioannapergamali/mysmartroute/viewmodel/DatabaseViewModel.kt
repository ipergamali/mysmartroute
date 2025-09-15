package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.WalkingRouteDatabase
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
import java.util.Locale

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
        private const val SYNC_TIMEOUT_MS = 120_000L
    }

    private val auth = FirebaseAuth.getInstance()

    private val _localData = MutableStateFlow<DatabaseData?>(null)
    val localData: StateFlow<DatabaseData?> = _localData

    private val _firebaseData = MutableStateFlow<DatabaseData?>(null)
    val firebaseData: StateFlow<DatabaseData?> = _firebaseData

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _currentSyncMessage = MutableStateFlow<String?>(null)
    val currentSyncMessage: StateFlow<String?> = _currentSyncMessage

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    @Volatile
    private var activeSyncStep: String = "Idle"

    private val localTableDefinitions = listOf(
        "users",
        "vehicles",
        "pois",
        "poi_types",
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

    private val tableNames = mapOf(
        "users" to TableNames("Χρήστες", "Users"),
        "vehicles" to TableNames("Οχήματα", "Vehicles"),
        "poi_types" to TableNames("Τύποι σημείων ενδιαφέροντος", "PoI Types"),
        "pois" to TableNames("Σημεία ενδιαφέροντος", "Points of Interest"),
        "settings" to TableNames("Ρυθμίσεις", "Settings"),
        "roles" to TableNames("Ρόλοι", "Roles"),
        "menus" to TableNames("Μενού", "Menus"),
        "menu_options" to TableNames("Επιλογές μενού", "Menu Options"),
        "app_language" to TableNames("Γλώσσα εφαρμογής", "App Language"),
        "routes" to TableNames("Διαδρομές", "Routes"),
        "route_points" to TableNames("Σημεία διαδρομών", "Route Points"),
        "route_bus_station" to TableNames("Στάσεις λεωφορείου διαδρομής", "Route Bus Stations"),
        "movings" to TableNames("Μετακινήσεις", "Movings"),
        "moving_details" to TableNames("Λεπτομέρειες μετακινήσεων", "Moving Details"),
        "walking" to TableNames("Πεζοπορίες", "Walking"),
        "walking_routes" to TableNames("Διαδρομές πεζοπορίας", "Walking Routes"),
        "transport_declarations" to TableNames("Δηλώσεις μεταφορών", "Transport Declarations"),
        "transport_declarations_details" to TableNames("Λεπτομέρειες δηλώσεων μεταφορών", "Transport Declaration Details"),
        "availabilities" to TableNames("Διαθεσιμότητες", "Availabilities"),
        "seat_reservations" to TableNames("Κρατήσεις θέσεων", "Seat Reservations"),
        "seat_reservation_details" to TableNames("Λεπτομέρειες κρατήσεων θέσεων", "Seat Reservation Details"),
        "favorites" to TableNames("Αγαπημένα", "Favorites"),
        "favorite_routes" to TableNames("Αγαπημένες διαδρομές", "Favorite Routes"),
        "transfer_requests" to TableNames("Αιτήματα μεταφοράς", "Transfer Requests"),
        "trip_ratings" to TableNames("Αξιολογήσεις διαδρομών", "Trip Ratings"),
        "notifications" to TableNames("Ειδοποιήσεις", "Notifications"),
        "user_pois" to TableNames("Σημεία ενδιαφέροντος χρηστών", "User PoIs"),
        "app_datetime" to TableNames("Ημερομηνία/ώρα συστήματος", "System Date/Time"),
        "user_settings" to TableNames("Ρυθμίσεις χρήστη", "User Settings")
    )

    private fun tableNamesFor(id: String): TableNames {
        return tableNames[id] ?: run {
            val english = id.split('_').joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            TableNames(english, english)
        }
    }

    private fun tableLabelFor(id: String): String {
        val names = tableNamesFor(id)
        return "${names.greek} / ${names.english}"
    }

    private fun progressMessage(context: Context, id: String): String {
        val names = tableNamesFor(id)
        return context.getString(R.string.clear_progress_message, names.greek, names.english)
    }

    private val _localTables = MutableStateFlow(localTableDefinitions.map { TableToggleState(id = it, title = tableLabelFor(it)) })
    val localTables: StateFlow<List<TableToggleState>> = _localTables

    private val _firebaseTables = MutableStateFlow(
        firebaseTableDefinitions.map { TableToggleState(id = it.collection, title = tableLabelFor(it.collection)) }
    )
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
            successMessage = context.getString(R.string.clear_success),
            preserveUserId = auth.currentUser?.uid
        )
    }

    fun clearAllTables(context: Context) {
        clearTables(
            context = context,
            localToClear = localTableDefinitions,
            firebaseToClear = firebaseTableDefinitions.map { it.collection },
            successMessage = context.getString(R.string.initialize_success),
            preserveUserId = auth.currentUser?.uid
        )
    }

    private fun clearTables(
        context: Context,
        localToClear: List<String>,
        firebaseToClear: List<String>,
        successMessage: String,
        preserveUserId: String?
    ) {
        Log.d(
            TAG,
            "Requested clearTables with local=$localToClear firebase=$firebaseToClear preserveUserId=${preserveUserId ?: "-"}"
        )
        viewModelScope.launch {
            _clearState.value = ClearState.Running(message = null)
            try {
                val db = MySmartRouteDatabase.getInstance(context)
                localToClear.forEach { table ->
                    _clearState.value = ClearState.Running(progressMessage(context, table))
                    try {
                        clearLocalTable(context, db, table, preserveUserId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clear local table $table", e)
                        throw e
                    }
                }
                firebaseToClear.forEach { table ->
                    _clearState.value = ClearState.Running(progressMessage(context, table))
                    val definition = firebaseDefinitionsById[table] ?: FirebaseTableDefinition(collection = table)
                    try {
                        clearFirebaseCollection(definition, preserveUserId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clear Firebase collection ${definition.collection}", e)
                        throw e
                    }
                }
                _localTables.update { tables -> tables.map { it.copy(selected = false) } }
                _firebaseTables.update { tables -> tables.map { it.copy(selected = false) } }
                _clearState.value = ClearState.Success(successMessage)
                Log.d(TAG, "clearTables completed successfully")
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

    private suspend fun clearLocalTable(
        context: Context,
        db: MySmartRouteDatabase,
        tableName: String,
        preserveUserId: String?
    ) {
        if (tableName == "walking_routes") {
            clearWalkingRoutes(context)
            return
        }
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val sqliteDb = db.openHelper.writableDatabase
                val before = sqliteDb.countRows(tableName)
                Log.d(TAG, "Clearing local table $tableName (rowsBefore=$before)")
                val shouldPreserveUser = tableName == "users" && !preserveUserId.isNullOrBlank()
                if (tableName == "poi_types") {
                    val dependentCount = sqliteDb.countRows("pois")
                    if (dependentCount > 0) {
                        val target = tableNamesFor(tableName)
                        val dependency = tableNamesFor("pois")
                        val message = context.getString(
                            R.string.clear_error_dependency,
                            target.greek,
                            target.english,
                            dependency.greek,
                            dependency.english,
                            dependentCount
                        )
                        throw IllegalStateException(message)
                    }
                }
                if (shouldPreserveUser) {
                    sqliteDb.execSQL(
                        "DELETE FROM `$tableName` WHERE id != ?",
                        arrayOf(preserveUserId)
                    )
                    val afterPreserve = sqliteDb.countRows(tableName)
                    Log.d(
                        TAG,
                        "Preserved user $preserveUserId in $tableName (rowsAfter=$afterPreserve)"
                    )
                } else {
                    sqliteDb.execSQL("DELETE FROM `$tableName`")
                    Log.d(TAG, "Executed DELETE FROM `$tableName`")
                }
                val after = sqliteDb.countRows(tableName)
                Log.d(TAG, "Cleared local table $tableName (rowsAfter=$after)")
            }
        }
    }

    private suspend fun clearWalkingRoutes(context: Context) {
        withContext(Dispatchers.IO) {
            val walkingDb = WalkingRouteDatabase.getDatabase(context)
            walkingDb.withTransaction {
                val sqliteDb = walkingDb.openHelper.writableDatabase
                val before = sqliteDb.countRows("walking_routes")
                Log.d(TAG, "Clearing local table walking_routes (rowsBefore=$before)")
                sqliteDb.execSQL("DELETE FROM `walking_routes`")
                val after = sqliteDb.countRows("walking_routes")
                Log.d(TAG, "Cleared local table walking_routes (rowsAfter=$after)")
            }
        }
    }

    private fun SupportSQLiteDatabase.countRows(tableName: String): Long {
        query("SELECT COUNT(*) FROM `$tableName`").use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    private suspend fun clearFirebaseCollection(
        definition: FirebaseTableDefinition,
        preserveUserId: String?
    ) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Clearing Firebase collection ${definition.collection}")
            val snapshot = firestore.collection(definition.collection).get().await()
            Log.d(
                TAG,
                "Fetched ${snapshot.documents.size} documents from ${definition.collection} for clearing"
            )
            var deletedCount = 0
            for (doc in snapshot.documents) {
                val preserveDocument = definition.collection == "users" && !preserveUserId.isNullOrBlank() && doc.id == preserveUserId
                Log.d(
                    TAG,
                    "Processing document ${doc.id} in ${definition.collection} (preserve=$preserveDocument)"
                )
                deleteSubcollections(doc.reference, definition.subcollections)
                if (!preserveDocument) {
                    doc.reference.delete().await()
                    deletedCount++
                    Log.d(TAG, "Deleted document ${doc.id} from ${definition.collection}")
                } else {
                    Log.d(TAG, "Skipping deletion for preserved document ${doc.id}")
                }
            }
            Log.d(TAG, "Deleted $deletedCount documents from ${definition.collection}")
        }
    }

    private suspend fun deleteSubcollections(
        document: DocumentReference,
        subcollections: List<SubcollectionDefinition>
    ) {
        if (subcollections.isEmpty()) return
        subcollections.forEach { subcollection ->
            val subSnapshot = document.collection(subcollection.name).get().await()
            Log.d(
                TAG,
                "Clearing subcollection ${subcollection.name} for document ${document.id} with ${subSnapshot.documents.size} entries"
            )
            for (doc in subSnapshot.documents) {
                deleteSubcollections(doc.reference, subcollection.children)
                doc.reference.delete().await()
                Log.d(TAG, "Deleted document ${doc.id} from subcollection ${subcollection.name}")
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
    fun loadFirebaseData(context: Context) {
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
                updateSyncTable(context, "roles")
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
                    updateSyncTable(context, "menus")
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
                        updateSyncTable(context, "menu_options")
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

            updateSyncTable(context, "routes")
            val routeTriples = firestore.collection("routes").get().await()
                .documents.mapNotNull { it.toRouteWithStations() }
            val routes = routeTriples.map { it.first }
            val routePoints = routeTriples.flatMap { it.second }
            val busStations = routeTriples.flatMap { it.third }

            updateSyncTable(context, "movings")
            val movings = firestore.collection("movings").get().await()
                .documents.mapNotNull { it.toMovingEntity() }

            updateSyncTable(context, "transport_declarations")
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
        logStep("Έναρξη συγχρονισμού βάσεων")
        if (!NetworkUtils.isInternetAvailable(context)) {
            logStep("Αποτυχία ελέγχου σύνδεσης (δεν υπάρχει δίκτυο)")
            _syncState.value = SyncState.Error("No internet connection")
            return
        }
        logStep("Διαθέσιμη σύνδεση στο διαδίκτυο")
        _syncState.value = SyncState.Loading
        updateSyncMessage(context.getString(R.string.sync_loading))
        val prefs = context.getSharedPreferences("db_sync", Context.MODE_PRIVATE)
        logStep("Ανάγνωση τοπικού timestamp")
        val localTs = prefs.getLong("last_sync", 0L)
        Log.d(TAG, "Local timestamp: $localTs")
        logStep("Ανάκτηση timestamp από Firestore")
        val remoteTs = try {
            firestore.collection("metadata").document("sync").get().await()
                .getLong("last_sync")?.also { Log.d(TAG, "Remote timestamp: $it") } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote timestamp", e)
            0L
        }

        logStep("Έναρξη διαδικασίας συγχρονισμού (localTs=$localTs remoteTs=$remoteTs)")

        val db = MySmartRouteDatabase.getInstance(context)

        try {
            withTimeout(SYNC_TIMEOUT_MS) {
                if (remoteTs > localTs) {
                    logStep("Το Firestore είναι νεότερο - λήψη δεδομένων")
                    Log.d(TAG, "Fetching users from Firestore")
                    updateSyncTable(context, "users")
                    val users = firestore.collection("users").get().await()
                        .documents.mapNotNull { it.toUserEntity() }
                    Log.d(TAG, "Fetched ${users.size} users")
                    Log.d(TAG, "Fetching vehicles from Firestore")
                    updateSyncTable(context, "vehicles")
                    val vehicles = firestore.collection("vehicles").get().await()
                        .documents.mapNotNull { doc -> doc.toVehicleEntity() }
                    Log.d(TAG, "Fetching PoIs from Firestore")
                    updateSyncTable(context, "pois")
                    val pois = firestore.collection("pois").get().await()
                        .documents.mapNotNull { it.toPoIEntity() }
                    Log.d(TAG, "Fetched ${pois.size} pois")
                    Log.d(TAG, "Fetching PoiTypes from Firestore")
                    updateSyncTable(context, "poi_types")
                    val poiTypes = firestore.collection("poi_types").get().await()
                        .documents.mapNotNull { doc: com.google.firebase.firestore.DocumentSnapshot ->
                            doc.toPoiTypeEntity()
                        }
                    Log.d(TAG, "Fetched ${poiTypes.size} poi types")
                    Log.d(TAG, "Fetching settings from Firestore")
                    updateSyncTable(context, "user_settings")
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
                    updateSyncTable(context, "roles")
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
                        updateSyncTable(context, "menus")
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
                            updateSyncTable(context, "menu_options")
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

                    updateSyncTable(context, "routes")
                    val routeTriples = firestore.collection("routes").get().await()
                        .documents.mapNotNull { it.toRouteWithStations() }
                    val routes = routeTriples.map { it.first }
                    val routePoints = routeTriples.flatMap { it.second }
                    val busStations = routeTriples.flatMap { it.third }

                    updateSyncTable(context, "movings")
                    val movingSnap = firestore.collection("movings").get().await()
                    val movingDetails = mutableListOf<MovingDetailEntity>()
                    val movings = movingSnap.documents.mapNotNull { doc ->
                        val moving = doc.toMovingEntity()
                        if (moving != null) {
                            updateSyncTable(context, "moving_details")
                            val dets = doc.reference.collection("details").get().await()
                            movingDetails += dets.documents.mapNotNull { it.toMovingDetailEntity(moving.id) }
                        }
                        moving
                    }

                    updateSyncTable(context, "transport_declarations")
                    val declSnap = firestore.collection("transport_declarations").get().await()
                    val declarations = mutableListOf<TransportDeclarationEntity>()
                    val declDetails = mutableListOf<TransportDeclarationDetailEntity>()
                    for (doc in declSnap.documents) {
                        val decl = doc.toTransportDeclarationEntity() ?: continue
                        updateSyncTable(context, "transport_declarations_details")
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

                    updateSyncTable(context, "availabilities")
                    val availabilities = firestore.collection("availabilities").get().await()
                        .documents.mapNotNull { it.toAvailabilityEntity() }

                    updateSyncTable(context, "favorites")
                    val favorites = favoritesGroup
                        .get()
                        .await()
                        .documents.mapNotNull { it.toFavoriteEntity() }

                    updateSyncTable(context, "seat_reservations")
                    val seatResSnap = firestore.collection("seat_reservations").get().await()
                    val seatResDetails = mutableListOf<SeatReservationDetailEntity>()
                    val seatReservations = seatResSnap.documents.mapNotNull { doc ->
                        val res = doc.toSeatReservationEntity()
                        if (res != null) {
                            updateSyncTable(context, "seat_reservation_details")
                            val dets = doc.reference.collection("details").get().await()
                            seatResDetails += dets.documents.mapNotNull { it.toSeatReservationDetailEntity(res.id) }
                        }
                        res
                    }

                    updateSyncTable(context, "transfer_requests")
                    val transferRequests = firestore.collection("transfer_requests").get().await()
                        .documents.mapNotNull { it.toTransferRequestEntity() }

                    updateSyncTable(context, "trip_ratings")
                    val tripRatings = firestore.collection("trip_ratings").get().await()
                        .documents.mapNotNull { it.toTripRatingEntity() }

                    updateSyncTable(context, "user_pois")
                    val userPois = emptyList<UserPoiEntity>()

                    Log.d(
                        TAG,
                        "Remote data -> users:${users.size} vehicles:${vehicles.size} pois:${pois.size} poiTypes:${poiTypes.size} settings:${settings.size} roles:${roles.size} menus:${menus.size} options:${menuOptions.size} routes:${routes.size} movings:${movings.size} declarations:${declarations.size} availabilities:${availabilities.size} favorites:${favorites.size} seatRes:${seatReservations.size} transferReq:${transferRequests.size} tripRatings:${tripRatings.size}"
                    )
                    updateSyncTable(context, "users")
                    users.forEach { insertUserSafely(db.userDao(), it) }
                    updateSyncTable(context, "vehicles")
                    vehicles.forEach { insertVehicleSafely(db, it) }
                    updateSyncTable(context, "pois")
                    pois.forEach { db.poIDao().insert(it) }
                    updateSyncTable(context, "poi_types")
                    db.poiTypeDao().insertAll(poiTypes)
                    updateSyncTable(context, "settings")
                    settings.forEach { insertSettingsSafely(db.settingsDao(), db.userDao(), it) }
                    updateSyncTable(context, "roles")
                    roles.forEach { db.roleDao().insert(it) }
                    updateSyncTable(context, "menus")
                    menus.forEach { insertMenuSafely(db.menuDao(), db.roleDao(), it) }
                    updateSyncTable(context, "menu_options")
                    menuOptions.forEach { db.menuOptionDao().insert(it) }
                    updateSyncTable(context, "routes")
                    routes.forEach { db.routeDao().insert(it) }
                    updateSyncTable(context, "route_points")
                    routePoints.forEach { db.routePointDao().insert(it) }
                    updateSyncTable(context, "route_bus_station")
                    busStations.forEach { db.routeBusStationDao().insert(it) }
                    updateSyncTable(context, "movings")
                    movings.forEach { db.movingDao().insert(it) }
                    updateSyncTable(context, "moving_details")
                    movingDetails.forEach { db.movingDetailDao().insert(it) }
                    updateSyncTable(context, "transport_declarations")
                    declarations.forEach { db.transportDeclarationDao().insert(it) }
                    if (declDetails.isNotEmpty()) {
                        updateSyncTable(context, "transport_declarations_details")
                        db.transportDeclarationDetailDao().insertAll(declDetails)
                    }
                    updateSyncTable(context, "availabilities")
                    availabilities.forEach { db.availabilityDao().insert(it) }
                    updateSyncTable(context, "favorites")
                    favorites.forEach { insertFavoriteSafely(db.favoriteDao(), db.userDao(), it) }
                    updateSyncTable(context, "seat_reservations")
                    seatReservations.forEach { db.seatReservationDao().insert(it) }
                    updateSyncTable(context, "seat_reservation_details")
                    seatResDetails.forEach { db.seatReservationDetailDao().insert(it) }
                    updateSyncTable(context, "transfer_requests")
                    transferRequests.forEach { db.transferRequestDao().insert(it) }
                    updateSyncTable(context, "trip_ratings")
                    tripRatings.forEach { db.tripRatingDao().upsert(it) }
                    Log.d(TAG, "Inserted remote data to local DB")
                    logStep("Ολοκληρώθηκε η ενημέρωση της τοπικής βάσης από Firestore")
                    prefs.edit().putLong("last_sync", remoteTs).apply()
                    _lastSyncTime.value = remoteTs
                } else {
                    logStep("Η τοπική βάση είναι νεότερη - αποστολή δεδομένων")
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
                    updateSyncTable(context, "users")
                    users.filter { it.id.isNotBlank() && it.name.isNotBlank() }.forEach { user ->
                        firestore.collection("users")
                            .document(user.id)
                            .set(user.toFirestoreMap()).await()
                    }
                    updateSyncTable(context, "vehicles")
                    vehicles.forEach {
                        firestore.collection("vehicles")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    updateSyncTable(context, "pois")
                    pois.forEach {
                        firestore.collection("pois").document(it.id).set(it.toFirestoreMap()).await()
                    }
                    updateSyncTable(context, "poi_types")
                    poiTypes.forEach {
                        firestore.collection("poi_types")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    updateSyncTable(context, "user_settings")
                    settings.forEach {
                        firestore.collection("user_settings")
                            .document(it.userId)
                            .set(it.toFirestoreMap()).await()
                    }
                    updateSyncTable(context, "roles")
                    roles.forEach {
                        firestore.collection("roles")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    updateSyncTable(context, "menus")
                    menus.forEach { menu ->
                        val ref = firestore.collection("roles")
                            .document(menu.roleId)
                            .collection("menus")
                            .document(menu.id)
                        ref.set(menu.toFirestoreMap()).await()
                        menuOptions.filter { it.menuId == menu.id }.forEach { opt ->
                            updateSyncTable(context, "menu_options")
                            ref.collection("options")
                                .document(opt.id)
                                .set(opt.toFirestoreMap()).await()
                        }
                    }

                    updateSyncTable(context, "routes")
                    routes.forEach { route ->
                        val points = routePoints.filter { it.routeId == route.id }
                        firestore.collection("routes")
                            .document(route.id)
                            .set(route.toFirestoreMap(points)).await()
                    }

                    updateSyncTable(context, "movings")
                    movings.forEach { moving ->
                        val ref = firestore.collection("movings").document(moving.id)
                        ref.set(moving.toFirestoreMap()).await()
                        movingDetailDao.getForMoving(moving.id).first().forEach { detail ->
                            updateSyncTable(context, "moving_details")
                            ref.collection("details")
                                .document(detail.id)
                                .set(detail.toFirestoreMap()).await()
                        }
                    }

                    updateSyncTable(context, "transport_declarations")
                    declarations.forEach { decl ->
                        firestore.collection("transport_declarations")
                            .document(decl.id)
                            .set(decl.toFirestoreMap()).await()
                        val details = detailDao.getForDeclaration(decl.id)
                        details.forEach { detail ->
                            updateSyncTable(context, "transport_declarations_details")
                            firestore.collection("transport_declarations")
                                .document(decl.id)
                                .collection("details")
                                .document(detail.id)
                                .set(detail.toFirestoreMap()).await()
                        }
                    }

                    updateSyncTable(context, "availabilities")
                    availabilities.forEach {
                        firestore.collection("availabilities")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }
                    updateSyncTable(context, "favorites")
                    favorites.forEach { fav ->
                        userVehicles(fav.userId)
                            .document(fav.id)
                            .set(fav.toFirestoreMap()).await()
                    }

                    updateSyncTable(context, "seat_reservations")
                    seatReservations.forEach { res ->
                        val ref = firestore.collection("seat_reservations").document(res.id)
                        ref.set(res.toFirestoreMap()).await()
                        seatResDetailDao.getForReservation(res.id).first().forEach { detail ->
                            updateSyncTable(context, "seat_reservation_details")
                            ref.collection("details")
                                .document(detail.id)
                                .set(detail.toFirestoreMap()).await()
                        }
                    }

                    updateSyncTable(context, "transfer_requests")
                    transferRequests.forEach {
                        firestore.collection("transfer_requests")
                            .document(it.requestNumber.toString())
                            .set(it.toFirestoreMap()).await()
                    }

                    updateSyncTable(context, "trip_ratings")
                    tripRatings.forEach {
                        firestore.collection("trip_ratings")
                            .document("${'$'}{it.movingId}_${'$'}{it.userId}")
                            .set(it.toFirestoreMap()).await()
                    }

                    updateSyncTable(context, "notifications")
                    notifications.forEach {
                        firestore.collection("notifications")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }

                    logStep("Ολοκληρώθηκε η αποστολή τοπικών δεδομένων στο Firestore")

                    val newTs = System.currentTimeMillis()
                    firestore.collection("metadata").document("sync").set(mapOf("last_sync" to newTs)).await()
                    prefs.edit().putLong("last_sync", newTs).apply()
                    _lastSyncTime.value = newTs
                }
            }
            logStep("Ο συγχρονισμός ολοκληρώθηκε επιτυχώς")
            _syncState.value = SyncState.Success
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Sync timeout στο στάδιο: $activeSyncStep", e)
            _syncState.value = SyncState.Error("Sync timeout")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error στο στάδιο: $activeSyncStep", e)
            _syncState.value = SyncState.Error(
                e.localizedMessage ?: "Sync failed"
            )
        } finally {
            if (_syncState.value !is SyncState.Loading) {
                updateSyncMessage(null)
            }
        }
    }

    private fun updateSyncMessage(message: String?) {
        _currentSyncMessage.value = message
    }

    private fun logStep(step: String) {
        activeSyncStep = step
        Log.d(TAG, "[Sync] $step")
    }

    private fun updateSyncTable(context: Context, tableId: String) {
        val tableName = context.tableDisplayName(tableId)
        logStep("Συγχρονισμός πίνακα $tableId ($tableName)")
        updateSyncMessage(context.getString(R.string.syncing_table, tableName))
    }

    private fun Context.tableDisplayName(tableId: String): String = when (tableId) {
        "users" -> getString(R.string.table_name_users)
        "vehicles" -> getString(R.string.table_name_vehicles)
        "pois" -> getString(R.string.table_name_pois)
        "poi_types" -> getString(R.string.table_name_poi_types)
        "settings", "user_settings" -> getString(R.string.table_name_settings)
        "roles" -> getString(R.string.table_name_roles)
        "menus" -> getString(R.string.table_name_menus)
        "menu_options" -> getString(R.string.table_name_menu_options)
        "app_language" -> getString(R.string.table_name_languages)
        "routes" -> getString(R.string.table_name_routes)
        "route_points" -> getString(R.string.table_name_route_points)
        "route_bus_station" -> getString(R.string.table_name_route_bus_stations)
        "movings" -> getString(R.string.table_name_movings)
        "moving_details" -> getString(R.string.table_name_moving_details)
        "transport_declarations" -> getString(R.string.table_name_transport_declarations)
        "transport_declarations_details" -> getString(R.string.table_name_transport_declaration_details)
        "availabilities" -> getString(R.string.table_name_availabilities)
        "favorites" -> getString(R.string.table_name_favorites)
        "favorite_routes" -> getString(R.string.table_name_favorite_routes)
        "user_pois" -> getString(R.string.table_name_user_pois)
        "walking" -> getString(R.string.table_name_walking)
        "walking_routes" -> getString(R.string.table_name_walking_routes)
        "seat_reservations" -> getString(R.string.table_name_seat_reservations)
        "seat_reservation_details" -> getString(R.string.table_name_seat_reservation_details)
        "transfer_requests" -> getString(R.string.table_name_transfer_requests)
        "trip_ratings" -> getString(R.string.table_name_trip_ratings)
        "notifications" -> getString(R.string.table_name_notifications)
        "app_datetime" -> getString(R.string.table_name_app_datetime)
        else -> tableId.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

}

data class TableToggleState(
    val id: String,
    val title: String,
    val selected: Boolean = false
)

data class TableNames(
    val greek: String,
    val english: String
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
    data class Running(val message: String?) : ClearState()
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
