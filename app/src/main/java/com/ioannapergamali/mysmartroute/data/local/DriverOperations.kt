package com.ioannapergamali.mysmartroute.data.local

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Καθαρίζει όλα τα δεδομένα ενός οδηγού από τη βάση Room και το Firebase Firestore.
 */
suspend fun demoteDriverToPassenger(
    db: MySmartRouteDatabase,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    driverId: String,
) = withContext(Dispatchers.IO) {
    // Τοπική βάση
    db.vehicleDao().deleteForUser(driverId)
    db.transportDeclarationDao().deleteForDriver(driverId)
    db.transferRequestDao().deleteForDriver(driverId)
    db.movingDao().deleteForDriver(driverId)

    // Firebase
    val batch = firestore.batch()

    val userRef = firestore.collection("users").document(driverId)

    firestore.collection("vehicles")
        .whereEqualTo("userId", userRef)
        .get().await()
        .forEach { batch.delete(it.reference) }

    firestore.collection("transport_declarations")
        .whereEqualTo("driverId", userRef)
        .get().await()
        .forEach { batch.delete(it.reference) }

    firestore.collection("transfer_requests")
        .whereEqualTo("driverId", userRef)
        .get().await()
        .forEach { batch.delete(it.reference) }

    firestore.collection("movings")
        .whereEqualTo("driverId", userRef)
        .get().await()
        .forEach { batch.delete(it.reference) }

    batch.commit().await()
}
