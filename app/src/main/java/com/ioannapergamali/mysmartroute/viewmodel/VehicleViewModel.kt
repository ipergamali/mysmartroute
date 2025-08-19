package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.insertVehicleSafely
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.VehiclePlacesUtils
import com.ioannapergamali.mysmartroute.model.classes.vehicles.RemoteVehicle
import com.ioannapergamali.mysmartroute.utils.toVehicleEntity
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class VehicleViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _vehicles = MutableStateFlow<List<VehicleEntity>>(emptyList())
    val vehicles: StateFlow<List<VehicleEntity>> = _vehicles

    private val _availableVehicles = MutableStateFlow<List<RemoteVehicle>>(emptyList())
    val availableVehicles: StateFlow<List<RemoteVehicle>> = _availableVehicles

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun registerVehicle(
        context: Context,
        name: String,
        description: String,
        type: VehicleType,
        seat: Int,
        color: String,
        plate: String
    ) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading

            val userId = auth.currentUser?.uid
            if (userId == null) {
                _registerState.value = RegisterState.Error("User not logged in")
                return@launch
            }

            if (name.isBlank()) {
                _registerState.value = RegisterState.Error("Name required")
                return@launch
            }

            if (description.isBlank()) {
                _registerState.value = RegisterState.Error("Description required")
                return@launch
            }

            if (plate.isBlank()) {
                _registerState.value = RegisterState.Error("License plate required")
                return@launch
            }

            if (seat <= 0) {
                _registerState.value = RegisterState.Error("Seats must be greater than 0")
                return@launch
            }

            if (color.isBlank()) {
                _registerState.value = RegisterState.Error("Color required")
                return@launch
            }

            val vehicleId = UUID.randomUUID().toString()
            val entity = VehicleEntity(vehicleId, name, description, userId, type.name, seat, color, plate)
            val vehicleData = entity.toFirestoreMap()

            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val vehicleDao = dbLocal.vehicleDao()
            val userDao = dbLocal.userDao()

            if (NetworkUtils.isInternetAvailable(context)) {
                db.collection("vehicles")
                    .document(vehicleId)
                    .set(vehicleData)
                    .addOnSuccessListener {
                        viewModelScope.launch { insertVehicleSafely(vehicleDao, userDao, entity) }
                        _registerState.value = RegisterState.Success
                    }
                    .addOnFailureListener { e ->
                        _registerState.value = RegisterState.Error(e.localizedMessage ?: "Failed")
                    }
            } else {
                insertVehicleSafely(vehicleDao, userDao, entity)
                _registerState.value = RegisterState.Success
            }
        }
    }

    fun loadRegisteredVehicles(
        context: Context,
        includeAll: Boolean = false,
        userId: String? = null
    ) {
        viewModelScope.launch {
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val vehicleDao = dbLocal.vehicleDao()
            val userDao = dbLocal.userDao()

            val targetUser = userId ?: auth.currentUser?.uid

            val remote = if (NetworkUtils.isInternetAvailable(context)) {
                val query = when {
                    includeAll -> db.collection("vehicles")
                    targetUser != null ->
                        db.collection("vehicles").whereEqualTo(
                            "userId",
                            db.collection("users").document(targetUser)
                        )
                    else -> null
                }
                query?.get()?.await()?.documents?.mapNotNull { it.toVehicleEntity() }
            } else null

            val local = when {
                includeAll -> vehicleDao.getAllVehicles().first()
                targetUser != null -> vehicleDao.getVehiclesForUser(targetUser)
                else -> emptyList()
            }

            var list = (remote ?: emptyList()) + local
            list = list.distinctBy { it.id }
            _vehicles.value = list
            list.forEach { insertVehicleSafely(vehicleDao, userDao, it) }
        }
    }

    fun loadAvailableVehicles(context: Context) {
        viewModelScope.launch {
            val apiKey = MapsUtils.getApiKey(context)
            _availableVehicles.value = VehiclePlacesUtils.fetchVehicles(apiKey)
        }
    }

    fun loadVehicleById(context: Context, vehicleId: String) {
        viewModelScope.launch {
            if (vehicleId.isBlank()) return@launch
            if (_vehicles.value.any { it.id == vehicleId }) return@launch

            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val vehicleDao = dbLocal.vehicleDao()
            val userDao = dbLocal.userDao()

            val local = vehicleDao.getVehicle(vehicleId)
            if (local != null) {
                _vehicles.value = _vehicles.value + local
                return@launch
            }

            if (NetworkUtils.isInternetAvailable(context)) {
                val doc = db.collection("vehicles").document(vehicleId).get().await()
                val entity = doc.toVehicleEntity()
                if (entity != null) {
                    insertVehicleSafely(vehicleDao, userDao, entity)
                    _vehicles.value = _vehicles.value + entity
                }
            }
        }
    }

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}
