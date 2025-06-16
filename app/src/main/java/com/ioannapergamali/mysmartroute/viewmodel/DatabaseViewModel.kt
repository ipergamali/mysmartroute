package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.insertSettingsSafely
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
                    val userId = doc.getString("userId") ?: return@mapNotNull null
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
                    val userId = doc.getString("userId") ?: return@mapNotNull null
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
            if (!NetworkUtils.isInternetAvailable(context)) {
                _syncState.value = SyncState.Error("No internet connection")
                return@launch
            }
            _syncState.value = SyncState.Loading
            val prefs = context.getSharedPreferences("db_sync", Context.MODE_PRIVATE)
            val localTs = prefs.getLong("last_sync", 0L)
            val remoteTs = try {
                firestore.collection("metadata").document("sync").get().await()
                    .getLong("last_sync") ?: 0L
            } catch (_: Exception) { 0L }

            val db = MySmartRouteDatabase.getInstance(context)

            try {
                if (remoteTs > localTs) {
                    val users = firestore.collection("users").get().await()
                        .documents.mapNotNull { it.toUserEntity() }
                    val vehicles = firestore.collection("vehicles").get().await()
                        .documents.mapNotNull { doc ->
                            val userRef = doc.getDocumentReference("userId") ?: return@mapNotNull null
                            VehicleEntity(
                                id = doc.getString("id") ?: "",
                                description = doc.getString("description") ?: "",
                                userId = userRef.id,
                                type = doc.getString("type") ?: "",
                                seat = (doc.getLong("seat") ?: 0L).toInt()
                            )
                        }
                    val pois = firestore.collection("pois").get().await()
                        .documents.mapNotNull { it.toObject(PoIEntity::class.java) }
                    val settings = firestore.collection("user_settings").get().await()
                        .documents.mapNotNull { doc ->
                            val userRef = doc.getDocumentReference("userId") ?: return@mapNotNull null
                            SettingsEntity(
                                userId = userRef.id,
                                theme = doc.getString("theme") ?: "",
                                darkTheme = doc.getBoolean("darkTheme") ?: false,
                                font = doc.getString("font") ?: "",
                                soundEnabled = doc.getBoolean("soundEnabled") ?: false,
                                soundVolume = (doc.getDouble("soundVolume") ?: 0.0).toFloat()
                            )
                        }
                    users.forEach { db.userDao().insert(it) }
                    vehicles.forEach { db.vehicleDao().insert(it) }
                    pois.forEach { db.poIDao().insert(it) }
                    settings.forEach { insertSettingsSafely(db.settingsDao(), db.userDao(), it) }
                    prefs.edit().putLong("last_sync", remoteTs).apply()
                } else {
                    val users = db.userDao().getAllUsers()
                    val vehicles = db.vehicleDao().getAllVehicles()
                    val pois = db.poIDao().getAll()
                    val settings = db.settingsDao().getAllSettings()

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

                    val newTs = System.currentTimeMillis()
                    firestore.collection("metadata").document("sync").set(mapOf("last_sync" to newTs)).await()
                    prefs.edit().putLong("last_sync", newTs).apply()
                }
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
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
