package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
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
                .documents.mapNotNull { it.toObject(UserEntity::class.java) }
            val vehicles = firestore.collection("vehicles").get().await()
                .documents.mapNotNull { it.toObject(VehicleEntity::class.java) }
            val pois = firestore.collection("pois").get().await()
                .documents.mapNotNull { it.toObject(PoIEntity::class.java) }
            val settings = firestore.collection("user_settings").get().await()
                .documents.mapNotNull { it.toObject(SettingsEntity::class.java) }
            _firebaseData.value = DatabaseData(users, vehicles, pois, settings)
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
