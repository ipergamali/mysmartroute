package com.ioannapergamali.mysmartroute.repository

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

    /**
     * Αποθηκεύει όχημα στο Firestore και τοπικά στη Room.
     */
    suspend fun addVehicle(vehicle: VehicleEntity) {
        firestore.collection("vehicles")
            .document(vehicle.id)
            .set(vehicle.toFirestoreMap())
            .await()
        insertVehicleSafely(vehicleDao, userDao, vehicle)
    }

    /**
     * Συγχρονίζει τοπική βάση με αλλαγές από το Firestore.
     */
    fun syncVehicles() {
        firestore.collection("vehicles")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val vehicles = snapshot.documents.mapNotNull { it.toVehicleEntity() }
                CoroutineScope(Dispatchers.IO).launch {
                    vehicles.forEach { insertVehicleSafely(vehicleDao, userDao, it) }
                }
            }
    }

    /**
     * Ροή με όλα τα οχήματα από τη Room.
     */
    fun getVehicles(): Flow<List<VehicleEntity>> = vehicleDao.getAllVehicles()
}

