package com.ioannapergamali.mysmartroute.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ioannapergamali.mysmartroute.data.local.VehicleDao
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.UserDao
import com.ioannapergamali.mysmartroute.data.local.insertVehicleSafely
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toVehicleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository για αποθήκευση και συγχρονισμό οχημάτων μεταξύ Firestore και Room.
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "VehicleRepo"
    }

    /**
     * Αποθηκεύει όχημα στο Firestore και τοπικά στη Room.
     */
    suspend fun addVehicle(vehicle: VehicleEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not logged in")
        val entity = vehicle.copy(userId = vehicle.userId.ifBlank { uid })

        Log.d(TAG, "Καταχώρηση οχήματος ${entity.id}")
        try {
            firestore.collection("vehicles")
                .document(entity.id)
                .set(entity.toFirestoreMap())
                .await()
            Log.d(TAG, "Αποθήκευση οχήματος ${entity.id} στο Firestore επιτυχής")
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία αποθήκευσης οχήματος ${entity.id} στο Firestore", e)
            throw e
        }
        insertVehicleSafely(vehicleDao, userDao, entity)
        Log.d(TAG, "Το όχημα ${entity.id} αποθηκεύτηκε τοπικά")
    }

    /** Ροή με όλα τα οχήματα από τη Room. */
    val vehicles: Flow<List<VehicleEntity>> = vehicleDao.getAllVehicles()

    /** Ροή με οχήματα συγκεκριμένου οδηγού. */
    fun vehiclesForUser(userId: String): Flow<List<VehicleEntity>> =
        vehicleDao.getVehiclesForUser(userId)

    private var registration: ListenerRegistration? = null

    /**
     * Εκκινεί συγχρονισμό Firestore → Room.
     */
    fun startSync(scope: CoroutineScope) {
        if (registration != null) return
        registration = firestore.collection("vehicles")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Log.e(TAG, "Σφάλμα συγχρονισμού", e)
                    return@addSnapshotListener
                }
                val vehicles = snapshot.documents.mapNotNull { it.toVehicleEntity() }
                scope.launch(Dispatchers.IO) {
                    vehicles.forEach { insertVehicleSafely(vehicleDao, userDao, it) }
                    Log.d(TAG, "Εισαγωγή ${vehicles.size} οχημάτων στη Room ολοκληρώθηκε")
                }
            }
    }

    fun stopSync() {
        registration?.remove()
        registration = null
    }
}

