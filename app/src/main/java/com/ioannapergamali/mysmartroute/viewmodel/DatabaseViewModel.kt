package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.insertSettingsSafely
import com.ioannapergamali.mysmartroute.data.local.insertVehicleSafely
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    fun loadLocalData(context: Context) {
        viewModelScope.launch {

            val db = MySmartRouteDatabase.getInstance(context)
            val users = db.userDao().getAllUsers()
            val vehicles = db.vehicleDao().getAllVehicles()
            val pois = db.poIDao().getAll()
            val settings = db.settingsDao().getAllSettings()
            _localData.value = DatabaseData(users, vehicles, pois, settings)
        }
    }

    fun loadFirebaseData() {
        viewModelScope.launch {
            val users = firestore.collection("users").get().await()
                .documents.mapNotNull { it.toUserEntity() }
            val vehicles = firestore.collection("vehicles").get().await()
                .documents.mapNotNull { doc ->
                    val userId = doc.getString("userId")
                        ?: doc.getDocumentReference("userId")?.id
                        ?: return@mapNotNull null
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
                    val userId = doc.getString("userId")
                        ?: doc.getDocumentReference("userId")?.id
                        ?: return@mapNotNull null
                    SettingsEntity(
                        userId = userId,
                        theme = doc.getString("theme") ?: "",
                        darkTheme = doc.getBoolean("darkTheme") ?: false,
                        font = doc.getString("font") ?: "",
                        soundEnabled = doc.getBoolean("soundEnabled") ?: false,
                        soundVolume = (doc.getDouble("soundVolume") ?: 0.0).toFloat()
                    )
                }
            _firebaseData.value = DatabaseData(users, vehicles, pois, settings)
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
                            val userId = doc.getString("userId")
                                ?: doc.getDocumentReference("userId")?.id
                                ?: return@mapNotNull null
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
                            val userId = doc.getString("userId")
                                ?: doc.getDocumentReference("userId")?.id
                                ?: return@mapNotNull null
                            SettingsEntity(
                                userId = userId,
                                theme = doc.getString("theme") ?: "",
                                darkTheme = doc.getBoolean("darkTheme") ?: false,
                                font = doc.getString("font") ?: "",
                                soundEnabled = doc.getBoolean("soundEnabled") ?: false,
                                soundVolume = (doc.getDouble("soundVolume") ?: 0.0).toFloat()
                            )
                        }

                    Log.d(
                        TAG,
                        "Remote data -> users:${'$'}{users.size} vehicles:${'$'}{vehicles.size} pois:${'$'}{pois.size} settings:${'$'}{settings.size}"
                    )
                    users.forEach { db.userDao().insert(it) }
                    vehicles.forEach { insertVehicleSafely(db.vehicleDao(), db.userDao(), it) }
                    pois.forEach { db.poIDao().insert(it) }
                    settings.forEach { insertSettingsSafely(db.settingsDao(), db.userDao(), it) }
                    Log.d(TAG, "Inserted remote data to local DB")
                    prefs.edit().putLong("last_sync", remoteTs).apply()
                } else {
                    Log.d(TAG, "Local database is newer, uploading data")
                    val users = db.userDao().getAllUsers()
                    Log.d(TAG, "Fetched ${users.size} local users")
                    val vehicles = db.vehicleDao().getAllVehicles()
                    Log.d(TAG, "Fetched ${vehicles.size} local vehicles")
                    val pois = db.poIDao().getAll()
                    Log.d(TAG, "Fetched ${pois.size} local pois")
                    val settings = db.settingsDao().getAllSettings()
                    Log.d(TAG, "Fetched ${settings.size} local settings")

                    Log.d(
                        TAG,
                        "Local data -> users:${'$'}{users.size} vehicles:${'$'}{vehicles.size} pois:${'$'}{pois.size} settings:${'$'}{settings.size}"
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

                    Log.d(TAG, "Uploaded local data to Firebase")

                    val newTs = System.currentTimeMillis()
                    firestore.collection("metadata").document("sync").set(mapOf("last_sync" to newTs)).await()
                    prefs.edit().putLong("last_sync", newTs).apply()
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
    val settings: List<SettingsEntity>
)

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
