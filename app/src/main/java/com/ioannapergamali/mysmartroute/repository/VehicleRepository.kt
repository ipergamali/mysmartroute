package com.ioannapergamali.mysmartroute.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.insertVehicleSafely
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toVehicleEntity
import com.ioannapergamali.mysmartroute.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository για αποθήκευση και συγχρονισμό οχημάτων μεταξύ Firestore και Room.
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val db: MySmartRouteDatabase,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "VehicleRepo"
    }

    private val vehicleDao = db.vehicleDao()

    /**
     * Αποθηκεύει όχημα στη Room και στη συνέχεια στο Firestore.
     */
    suspend fun addVehicle(vehicle: VehicleEntity) = withContext(Dispatchers.IO) {
        val uid = SessionManager.currentUserId()
            ?: throw IllegalStateException("User not logged in")
        val entity = vehicle.copy(userId = vehicle.userId.ifBlank { uid })

        Log.d(TAG, "Καταχώρηση οχήματος ${entity.id}")

        insertVehicleSafely(db, entity)
        Log.d(TAG, "Το όχημα ${entity.id} αποθηκεύτηκε τοπικά")

        try {
            firestore.collection("vehicles")
                .document(entity.id)
                .set(entity.toFirestoreMap())
                .await()
            Log.d(TAG, "Αποθήκευση οχήματος ${entity.id} στο Firestore επιτυχής")
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία αποθήκευσης οχήματος ${entity.id} στο Firestore", e)
        }
    }

    /**
     * Συγχρονίζει τα οχήματα του τρέχοντος χρήστη από το Firestore στη Room μία φορά.
     */
    suspend fun syncVehicles() = withContext(Dispatchers.IO) {
        val uid = SessionManager.currentUserId() ?: return@withContext
        val userRef = firestore.collection("users").document(uid)
        try {
            val snapshot = withTimeout(10_000) {
                firestore.collection("vehicles")
                    .whereEqualTo("userId", userRef)
                    .get()
                    .await()
            }
            val vehicles = snapshot.documents.mapNotNull { it.toVehicleEntity() }
            vehicles.forEach { insertVehicleSafely(db, it) }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Αποτυχία συγχρονισμού οχημάτων: Timeout", e)
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία συγχρονισμού οχημάτων", e)
        }
    }

    /** Ροή με όλα τα οχήματα. Αν η Room είναι κενή, τα φέρνει από το Firestore. */
    val vehicles: Flow<List<VehicleEntity>> = vehicleDao.getVehicles().onStart {
        val local = vehicleDao.getVehicles().first()
        val uid = SessionManager.currentUserId()
        if (local.isEmpty() && uid != null) {
            Log.d(TAG, "Τοπικά οχήματα κενά, ανάκτηση από Firestore")
            val userRef = firestore.collection("users").document(uid)
            val remote = firestore.collection("vehicles")
                .whereEqualTo("userId", userRef)
                .get()
                .await()
                .documents.mapNotNull { it.toVehicleEntity() }
            remote.forEach { insertVehicleSafely(db, it) }
            Log.d(TAG, "Εισαγωγή ${remote.size} οχημάτων από Firestore ολοκληρώθηκε")
        }
    }

    /** Ροή με οχήματα συγκεκριμένου οδηγού. */
    fun vehiclesForUser(userId: String): Flow<List<VehicleEntity>> =
        vehicleDao.getVehiclesForUser(userId)

    private var registration: ListenerRegistration? = null

    /**
     * Εκκινεί συγχρονισμό Firestore → Room.
     */
    fun startSync(scope: CoroutineScope) {
        if (registration != null) return
        val uid = SessionManager.currentUserId() ?: return
        val userRef = firestore.collection("users").document(uid)
        registration = firestore.collection("vehicles")
            .whereEqualTo("userId", userRef)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Log.e(TAG, "Σφάλμα συγχρονισμού", e)
                    return@addSnapshotListener
                }
                val vehicles = snapshot.documents.mapNotNull { it.toVehicleEntity() }
                scope.launch(Dispatchers.IO) {
                    vehicles.forEach { insertVehicleSafely(db, it) }
                    Log.d(TAG, "Εισαγωγή ${vehicles.size} οχημάτων στη Room ολοκληρώθηκε")
                }
            }
    }

    fun stopSync() {
        registration?.remove()
        registration = null
    }
}

