package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.PoiTypeEntity
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.insertSettingsSafely
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
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.data.local.AvailabilityEntity
import com.ioannapergamali.mysmartroute.utils.toRouteWithPoints
import com.ioannapergamali.mysmartroute.utils.toMovingEntity
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationEntity
import com.ioannapergamali.mysmartroute.utils.toAvailabilityEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** ViewModel για προβολή δεδομένων βάσεων. */
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

    fun loadLastSync(context: Context) {
        val prefs = context.getSharedPreferences("db_sync", Context.MODE_PRIVATE)
        _lastSyncTime.value = prefs.getLong("last_sync", 0L)
    }

    fun loadLocalData(context: Context) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            Log.d(TAG, "Loading local data")
            kotlinx.coroutines.flow.combine(
                db.userDao().getAllUsers(),
                db.vehicleDao().getAllVehicles(),
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
                db.favoriteDao().getAll()
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
                    favorites
                )
            }.collect { data ->
                Log.d(
                    TAG,
                    "Local data -> users:${data.users.size} vehicles:${data.vehicles.size} " +
                    "pois:${data.pois.size} poiTypes:${data.poiTypes.size} settings:${data.settings.size} roles:${data.roles.size} " +
                        "menus:${data.menus.size} options:${data.menuOptions.size} routes:${data.routes.size} " +
                    "points:${data.routePoints.size} movings:${data.movings.size} declarations:${data.declarations.size}" +
                        " availabilities:${data.availabilities.size} favorites:${data.favorites.size}"
                )
                _localData.value = data
            }
        }
    }

    fun loadFirebaseData() {
        viewModelScope.launch {
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

            val routePairs = firestore.collection("routes").get().await()
                .documents.mapNotNull { it.toRouteWithPoints() }
            val routes = routePairs.map { it.first }
            val routePoints = routePairs.flatMap { it.second }

            val movings = firestore.collection("movings").get().await()
                .documents.mapNotNull { it.toMovingEntity() }

            val declarations = firestore.collection("transport_declarations").get().await()
                .documents.mapNotNull { it.toTransportDeclarationEntity() }

            val availabilities = firestore.collection("availabilities").get().await()
                .documents.mapNotNull { it.toAvailabilityEntity() }

            val favorites = favoritesGroup
                .get()
                .await()
                .documents.mapNotNull { it.toFavoriteEntity() }

            Log.d(TAG, "Firebase data -> users:${users.size} vehicles:${vehicles.size} pois:${pois.size} types:${poiTypes.size} settings:${settings.size} roles:${roles.size} menus:${menus.size} options:${menuOptions.size} routes:${routes.size} movings:${movings.size} declarations:${declarations.size} availabilities:${availabilities.size} favorites:${favorites.size}")
            _firebaseData.value = DatabaseData(users, vehicles, pois, poiTypes, settings, roles, menus, menuOptions, emptyList(), routes, routePoints, movings, declarations, availabilities, favorites)
        }
    }

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

                    val routePairs = firestore.collection("routes").get().await()
                        .documents.mapNotNull { it.toRouteWithPoints() }
                    val routes = routePairs.map { it.first }
                    val routePoints = routePairs.flatMap { it.second }

                    val movings = firestore.collection("movings").get().await()
                        .documents.mapNotNull { it.toMovingEntity() }

                    val declarations = firestore.collection("transport_declarations").get().await()
                        .documents.mapNotNull { it.toTransportDeclarationEntity() }

                    val availabilities = firestore.collection("availabilities").get().await()
                        .documents.mapNotNull { it.toAvailabilityEntity() }

                    val favorites = favoritesGroup
                        .get()
                        .await()
                        .documents.mapNotNull { it.toFavoriteEntity() }

                    Log.d(
                        TAG,
                        "Remote data -> users:${users.size} vehicles:${vehicles.size} pois:${pois.size} poiTypes:${poiTypes.size} settings:${settings.size} roles:${roles.size} menus:${menus.size} options:${menuOptions.size} routes:${routes.size} movings:${movings.size} declarations:${declarations.size} availabilities:${availabilities.size} favorites:${favorites.size}"
                    )
                    users.forEach { db.userDao().insert(it) }
                    vehicles.forEach { insertVehicleSafely(db.vehicleDao(), db.userDao(), it) }
                    pois.forEach { db.poIDao().insert(it) }
                    db.poiTypeDao().insertAll(poiTypes)
                    settings.forEach { insertSettingsSafely(db.settingsDao(), db.userDao(), it) }
                    roles.forEach { db.roleDao().insert(it) }
                    menus.forEach { insertMenuSafely(db.menuDao(), db.roleDao(), it) }
                    menuOptions.forEach { db.menuOptionDao().insert(it) }
                    routes.forEach { db.routeDao().insert(it) }
                    routePoints.forEach { db.routePointDao().insert(it) }
                    movings.forEach { db.movingDao().insert(it) }
                    declarations.forEach { db.transportDeclarationDao().insert(it) }
                    availabilities.forEach { db.availabilityDao().insert(it) }
                    favorites.forEach { insertFavoriteSafely(db.favoriteDao(), db.userDao(), it) }
                    Log.d(TAG, "Inserted remote data to local DB")
                    prefs.edit().putLong("last_sync", remoteTs).apply()
                    _lastSyncTime.value = remoteTs
                } else {
                    Log.d(TAG, "Local database is newer, uploading data")
                    val users = db.userDao().getAllUsers().first()
                    Log.d(TAG, "Fetched ${users.size} local users")
                    val vehicles = db.vehicleDao().getAllVehicles().first()
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

                    val availabilities = db.availabilityDao().getAll().first()
                    Log.d(TAG, "Fetched ${availabilities.size} local availabilities")
                    val favorites = db.favoriteDao().getAll().first()
                    Log.d(TAG, "Fetched ${favorites.size} local favorites")

                    Log.d(
                        TAG,
                        "Local data -> users:${users.size} vehicles:${vehicles.size} pois:${pois.size} poiTypes:${poiTypes.size} settings:${settings.size} roles:${roles.size} menus:${menus.size} options:${menuOptions.size} routes:${routes.size} points:${routePoints.size} movings:${movings.size} declarations:${declarations.size} availabilities:${availabilities.size} favorites:${favorites.size}"
                    )

                    users.forEach {
                        firestore.collection("users")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
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

                    movings.forEach {
                        firestore.collection("movings")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
                    }

                    declarations.forEach {
                        firestore.collection("transport_declarations")
                            .document(it.id)
                            .set(it.toFirestoreMap()).await()
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
    val favorites: List<FavoriteEntity>
)

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
