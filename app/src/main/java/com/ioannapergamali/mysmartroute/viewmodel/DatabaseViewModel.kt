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
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.insertSettingsSafely
import com.ioannapergamali.mysmartroute.data.local.insertVehicleSafely
import com.ioannapergamali.mysmartroute.data.local.insertMenuSafely
import com.ioannapergamali.mysmartroute.data.local.RoleEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** ViewModel για προβολή δεδομένων βάσεων. */
class DatabaseViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

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
            kotlinx.coroutines.flow.combine(
                db.userDao().getAllUsers(),
                db.vehicleDao().getAllVehicles(),
                db.poIDao().getAll(),
                db.settingsDao().getAllSettings(),
                db.roleDao().getAllRoles(),
                db.menuDao().getAllMenus(),
                db.menuOptionDao().getAllMenuOptions()
            ) { values ->
                val users = values[0] as List<UserEntity>
                val vehicles = values[1] as List<VehicleEntity>
                val pois = values[2] as List<PoIEntity>
                val settings = values[3] as List<SettingsEntity>
                val roles = values[4] as List<RoleEntity>
                val menus = values[5] as List<MenuEntity>
                val options = values[6] as List<MenuOptionEntity>
                DatabaseData(users, vehicles, pois, settings, roles, menus, options)
            }.collect { data ->
                _localData.value = data
            }
        }
    }

    fun loadFirebaseData() {
        viewModelScope.launch {
            val users = firestore.collection("users").get().await()
                .documents.mapNotNull { it.toUserEntity() }
            val vehicles = firestore.collection("vehicles").get().await()
                .documents.mapNotNull { doc ->
                    val userId = when (val uid = doc.get("userId")) {
                        is String -> uid
                        is DocumentReference -> uid.id
                        else -> null
                    } ?: return@mapNotNull null
                    VehicleEntity(
                        id = doc.getString("id") ?: "",
                        description = doc.getString("description") ?: "",
                        userId = userId,
                        type = doc.getString("type") ?: "",
                        seat = (doc.getLong("seat") ?: 0L).toInt()
                    )
                }
            val pois = firestore.collection("pois").get().await()
                .documents.mapNotNull { it.toObject(PoIEntity::class.java) }
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
                        soundVolume = (doc.getDouble("soundVolume") ?: 0.0).toFloat()
                    )
                }

            val rolesSnap = firestore.collection("roles").get().await()
            val roles = rolesSnap.documents.map { doc ->
                RoleEntity(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: ""
                )
            }
            val menus = mutableListOf<MenuEntity>()
            val menuOptions = mutableListOf<MenuOptionEntity>()
            for (roleDoc in rolesSnap.documents) {
                val roleId = roleDoc.getString("id") ?: roleDoc.id
                val menusSnap = roleDoc.reference.collection("menus").get().await()
                for (menuDoc in menusSnap.documents) {
                    val menuId = menuDoc.getString("id") ?: menuDoc.id
                    menus.add(
                        MenuEntity(
                            id = menuId,
                            roleId = roleId,
                            title = menuDoc.getString("title") ?: ""
                        )
                    )
                    val optsSnap = menuDoc.reference.collection("options").get().await()
                    for (optDoc in optsSnap.documents) {
                        menuOptions.add(
                            MenuOptionEntity(
                                id = optDoc.getString("id") ?: optDoc.id,
                                menuId = menuId,
                                title = optDoc.getString("title") ?: "",
                                route = optDoc.getString("route") ?: ""
                            )
                        )
                    }
                }
            }

            _firebaseData.value = DatabaseData(users, vehicles, pois, settings, roles, menus, menuOptions)
        }
    }

    fun syncDatabases(context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Starting database synchronization")
            if (!NetworkUtils.isInternetAvailable(context)) {
                Log.w(TAG, "No internet connection")
                _syncState.value = SyncState.Error("No internet connection")
                return@launch
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
                if (remoteTs > localTs) {
                    Log.d(TAG, "Remote database is newer, downloading data")
                    Log.d(TAG, "Fetching users from Firestore")
                    val users = firestore.collection("users").get().await()
                        .documents.mapNotNull { it.toUserEntity() }
                    Log.d(TAG, "Fetched ${users.size} users")
                    Log.d(TAG, "Fetching vehicles from Firestore")
                    val vehicles = firestore.collection("vehicles").get().await()
                        .documents.mapNotNull { doc ->
                            val userId = when (val uid = doc.get("userId")) {
                                is String -> uid
                                is DocumentReference -> uid.id
                                else -> null
                            } ?: return@mapNotNull null
                            VehicleEntity(
                                id = doc.getString("id") ?: "",
                                description = doc.getString("description") ?: "",
                                userId = userId,
                                type = doc.getString("type") ?: "",
                                seat = (doc.getLong("seat") ?: 0L).toInt()
                            )
                        }
                    Log.d(TAG, "Fetching PoIs from Firestore")
                    val pois = firestore.collection("pois").get().await()
                        .documents.mapNotNull { it.toObject(PoIEntity::class.java) }
                    Log.d(TAG, "Fetched ${pois.size} pois")
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
                                soundVolume = (doc.getDouble("soundVolume") ?: 0.0).toFloat()
                            )
                        }

                    val rolesSnap = firestore.collection("roles").get().await()
                    val roles = rolesSnap.documents.map { doc ->
                        RoleEntity(
                            id = doc.getString("id") ?: doc.id,
                            name = doc.getString("name") ?: ""
                        )
                    }
                    val menus = mutableListOf<MenuEntity>()
                    val menuOptions = mutableListOf<MenuOptionEntity>()
                    for (roleDoc in rolesSnap.documents) {
                        val roleId = roleDoc.getString("id") ?: roleDoc.id
                        val menusSnap = roleDoc.reference.collection("menus").get().await()
                        for (menuDoc in menusSnap.documents) {
                            val menuId = menuDoc.getString("id") ?: menuDoc.id
                            menus.add(
                                MenuEntity(
                                    id = menuId,
                                    roleId = roleId,
                                    title = menuDoc.getString("title") ?: ""
                                )
                            )
                            val optsSnap = menuDoc.reference.collection("options").get().await()
                            for (optDoc in optsSnap.documents) {
                                menuOptions.add(
                                    MenuOptionEntity(
                                        id = optDoc.getString("id") ?: optDoc.id,
                                        menuId = menuId,
                                        title = optDoc.getString("title") ?: "",
                                        route = optDoc.getString("route") ?: ""
                                    )
                                )
                            }
                        }
                    }

                    Log.d(
                        TAG,
                        "Remote data -> users:${'$'}{users.size} vehicles:${'$'}{vehicles.size} pois:${'$'}{pois.size} settings:${'$'}{settings.size} roles:${'$'}{roles.size} menus:${'$'}{menus.size} options:${'$'}{menuOptions.size}"
                    )
                    users.forEach { db.userDao().insert(it) }
                    vehicles.forEach { insertVehicleSafely(db.vehicleDao(), db.userDao(), it) }
                    pois.forEach { db.poIDao().insert(it) }
                    settings.forEach { insertSettingsSafely(db.settingsDao(), db.userDao(), it) }
                    roles.forEach { db.roleDao().insert(it) }
                    menus.forEach { insertMenuSafely(db.menuDao(), db.roleDao(), it) }
                    menuOptions.forEach { db.menuOptionDao().insert(it) }
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
                    val settings = db.settingsDao().getAllSettings().first()
                    Log.d(TAG, "Fetched ${settings.size} local settings")
                    val roles = db.roleDao().getAllRoles().first()
                    Log.d(TAG, "Fetched ${roles.size} local roles")
                    val menus = db.menuDao().getAllMenus().first()
                    Log.d(TAG, "Fetched ${menus.size} local menus")
                    val menuOptions = db.menuOptionDao().getAllMenuOptions().first()
                    Log.d(TAG, "Fetched ${menuOptions.size} local options")

                    Log.d(
                        TAG,
                        "Local data -> users:${'$'}{users.size} vehicles:${'$'}{vehicles.size} pois:${'$'}{pois.size} settings:${'$'}{settings.size} roles:${'$'}{roles.size} menus:${'$'}{menus.size} options:${'$'}{menuOptions.size}"
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
                    pois.forEach { firestore.collection("pois").document(it.id).set(it).await() }
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

                    Log.d(TAG, "Uploaded local data to Firebase")

                    val newTs = System.currentTimeMillis()
                    firestore.collection("metadata").document("sync").set(mapOf("last_sync" to newTs)).await()
                    prefs.edit().putLong("last_sync", newTs).apply()
                    _lastSyncTime.value = newTs
                }
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Sync error", e)
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync failed")
            }
        }
    }
}

/** Δομή για τα δεδομένα κάθε βάσης. */
data class DatabaseData(
    val users: List<UserEntity>,
    val vehicles: List<VehicleEntity>,
    val pois: List<PoIEntity>,
    val settings: List<SettingsEntity>,
    val roles: List<RoleEntity>,
    val menus: List<MenuEntity>,
    val menuOptions: List<MenuOptionEntity>
)

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
