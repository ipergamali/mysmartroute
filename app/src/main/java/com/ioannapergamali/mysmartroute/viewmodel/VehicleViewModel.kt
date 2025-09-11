package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.insertVehicleSafely
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.VehiclePlacesUtils
import com.ioannapergamali.mysmartroute.model.classes.vehicles.RemoteVehicle
import com.ioannapergamali.mysmartroute.utils.toVehicleEntity
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.repository.VehicleRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.UUID

/**
 * ViewModel για εγγραφή και ανάκτηση οχημάτων από τη Room DB και το Firestore.
 * ViewModel for registering and fetching vehicles from Room DB and Firestore.
 */
class VehicleViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var repository: VehicleRepository? = null

    companion object {
        private const val TAG = "VehicleVM"
    }

    private val _vehicles = MutableStateFlow<List<VehicleEntity>>(emptyList())
    val vehicles: StateFlow<List<VehicleEntity>> = _vehicles
    private var vehiclesJob: Job? = null

    private val _availableVehicles = MutableStateFlow<List<RemoteVehicle>>(emptyList())
    val availableVehicles: StateFlow<List<RemoteVehicle>> = _availableVehicles

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    /**
     * Καταχωρεί νέο όχημα για τον τρέχοντα χρήστη και το αποθηκεύει.
     * Registers a new vehicle for the current user and stores it.
     */
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
            Log.d(TAG, "Έναρξη καταχώρησης οχήματος $name τύπος=$type")
            _registerState.value = RegisterState.Loading

            val userId = auth.currentUser?.uid
            if (userId == null) {
                _registerState.value = RegisterState.Error(
                    context.getString(R.string.user_not_logged_in)
                )
                return@launch
            }

            if (name.isBlank()) {
                _registerState.value = RegisterState.Error(
                    context.getString(R.string.name_required)
                )
                return@launch
            }

            if (description.isBlank()) {
                _registerState.value = RegisterState.Error(
                    context.getString(R.string.description_required)
                )
                return@launch
            }

            if (plate.isBlank()) {
                _registerState.value = RegisterState.Error(
                    context.getString(R.string.license_plate_required)
                )
                return@launch
            }

            if (seat <= 0) {
                _registerState.value = RegisterState.Error(
                    context.getString(R.string.seats_must_be_greater_than_zero)
                )
                return@launch
            }

            if (color.isBlank()) {
                _registerState.value = RegisterState.Error(
                    context.getString(R.string.color_required)
                )
                return@launch
            }

            val vehicleId = UUID.randomUUID().toString()
            val entity = VehicleEntity(vehicleId, name, description, userId, type.name, seat, color, plate)

            val repo = getRepository(context)
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val vehicleDao = dbLocal.vehicleDao()
            val userDao = dbLocal.userDao()

            if (NetworkUtils.isInternetAvailable(context)) {
                try {
                    repo.addVehicle(entity)
                    Log.d(TAG, "Το όχημα ${entity.id} αποθηκεύτηκε επιτυχώς")
                    _registerState.value = RegisterState.Success
                } catch (e: Exception) {
                    Log.e(TAG, "Αποτυχία καταχώρησης οχήματος", e)
                    _registerState.value = RegisterState.Error(e.localizedMessage ?: "Failed")
                }
            } else {
                insertVehicleSafely(vehicleDao, userDao, entity)
                Log.d(TAG, "Αποθήκευση οχήματος ${entity.id} μόνο τοπικά λόγω έλλειψης δικτύου")
                _registerState.value = RegisterState.Success
            }
        }
    }

    /**
     * Φορτώνει οχήματα που έχουν καταχωρηθεί, είτε όλα είτε για συγκεκριμένο χρήστη.
     * Loads registered vehicles, optionally for a specific user.
     */
    fun loadRegisteredVehicles(
        context: Context,
        includeAll: Boolean = false,
        userId: String? = null
    ) {
        val repo = getRepository(context)
        val uid = userId ?: auth.currentUser?.uid
        vehiclesJob?.cancel()
        vehiclesJob = viewModelScope.launch {
            when {
                includeAll -> repo.vehicles.collect { _vehicles.value = it }
                uid != null -> repo.vehiclesForUser(uid).collect { _vehicles.value = it }
                else -> _vehicles.value = emptyList()
            }
        }
    }

    /**
     * Ανακτά διαθέσιμα οχήματα από εξωτερική υπηρεσία.
     * Fetches available vehicles from an external service.
     */
    fun loadAvailableVehicles(context: Context) {
        viewModelScope.launch {
            val apiKey = MapsUtils.getApiKey(context)
            _availableVehicles.value = VehiclePlacesUtils.fetchVehicles(apiKey)
        }
    }

    /**
     * Φορτώνει όχημα βάσει αναγνωριστικού αν δεν υπάρχει ήδη στη μνήμη.
     * Loads a vehicle by ID if not already in memory.
     */
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

    private fun getRepository(context: Context): VehicleRepository {
        val existing = repository
        if (existing != null) return existing
        val dbLocal = MySmartRouteDatabase.getInstance(context)
        return VehicleRepository(dbLocal.vehicleDao(), dbLocal.userDao(), db).also { repo ->
            repository = repo
            repo.startSync(viewModelScope)
        }
    }

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}
