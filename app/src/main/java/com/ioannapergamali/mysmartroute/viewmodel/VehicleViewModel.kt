package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.authRef
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class VehicleViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _vehicles = MutableStateFlow<List<VehicleEntity>>(emptyList())
    val vehicles: StateFlow<List<VehicleEntity>> = _vehicles

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun registerVehicle(
        context: Context,
        description: String,
        type: VehicleType,
        seat: Int
    ) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading

            val userId = auth.currentUser?.uid
            if (userId == null) {
                _registerState.value = RegisterState.Error("User not logged in")
                return@launch
            }

            if (description.isBlank()) {
                _registerState.value = RegisterState.Error("Description required")
                return@launch
            }

            val vehicleId = UUID.randomUUID().toString()
            val vehicleData = hashMapOf(
                "id" to vehicleId,
                "description" to description,
                "userId" to db.authRef(userId),
                "type" to type.name,
                "seat" to seat
            )

            val dao = MySmartRouteDatabase.getInstance(context).vehicleDao()
            val entity = VehicleEntity(vehicleId, description, userId, type.name, seat)

            if (NetworkUtils.isInternetAvailable(context)) {
                db.collection("vehicles")
                    .document(vehicleId)
                    .set(vehicleData)
                    .addOnSuccessListener {
                        viewModelScope.launch { dao.insert(entity) }
                        _registerState.value = RegisterState.Success
                    }
                    .addOnFailureListener { e ->
                        _registerState.value = RegisterState.Error(e.localizedMessage ?: "Failed")
                    }
            } else {
                dao.insert(entity)
                _registerState.value = RegisterState.Success
            }
        }
    }

    fun loadRegisteredVehicles(context: Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val dao = MySmartRouteDatabase.getInstance(context).vehicleDao()
            _vehicles.value = dao.getVehiclesForUser(userId)
        }
    }

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}
