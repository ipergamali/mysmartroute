package com.ioannapergamali.mysmartroute.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
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
        Log.d(TAG, "Καταχώρηση οχήματος ${vehicle.id}")
        try {
            firestore.collection("vehicles")
                .document(vehicle.id)
                .set(vehicle.toFirestoreMap())
                .await()
            Log.d(TAG, "Αποθήκευση οχήματος ${vehicle.id} στο Firestore επιτυχής")
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία αποθήκευσης οχήματος ${vehicle.id} στο Firestore", e)
            throw e
        }
        insertVehicleSafely(vehicleDao, userDao, vehicle)
        Log.d(TAG, "Το όχημα ${vehicle.id} αποθηκεύτηκε τοπικά")
    }

    /**
     * Συγχρονίζει τοπική βάση με αλλαγές από το Firestore.
     */
    fun syncVehicles() {
        firestore.collection("vehicles")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Σφάλμα συγχρονισμού", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    Log.w(TAG, "Κενό snapshot για vehicles")
                    return@addSnapshotListener
                }
                Log.d(TAG, "Λήψη ${snapshot.documents.size} οχημάτων από Firestore")
                val vehicles = snapshot.documents.mapNotNull { it.toVehicleEntity() }
                CoroutineScope(Dispatchers.IO).launch {
                    vehicles.forEach { insertVehicleSafely(vehicleDao, userDao, it) }
                    Log.d(TAG, "Εισαγωγή ${vehicles.size} οχημάτων στη Room ολοκληρώθηκε")
                }
            }
    }

    /**
     * Ροή με όλα τα οχήματα από τη Room.
     */
    fun getVehicles(): Flow<List<VehicleEntity>> = vehicleDao.getAllVehicles()
}

