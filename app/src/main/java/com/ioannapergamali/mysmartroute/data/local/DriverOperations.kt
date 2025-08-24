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
    val userRef = firestore.collection("users").document(driverId)
    val batch = firestore.batch()

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

/**
 * Καθαρίζει τα εκκρεμή δεδομένα ενός επιβάτη όταν προάγεται σε οδηγό.
 */
suspend fun promotePassengerToDriver(
    db: MySmartRouteDatabase,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    passengerId: String,
) = withContext(Dispatchers.IO) {
    // Τοπική βάση
    db.transferRequestDao().deleteForPassenger(passengerId)
    db.movingDao().deleteForUser(passengerId)
    db.seatReservationDao().deleteForUser(passengerId)

    // Firebase
    val userRef = firestore.collection("users").document(passengerId)
    val batch = firestore.batch()

    firestore.collection("transfer_requests")
        .whereEqualTo("passengerId", userRef)
        .get().await()
        .forEach { batch.delete(it.reference) }

    firestore.collection("movings")
        .whereEqualTo("userId", userRef)
        .get().await()
        .forEach { batch.delete(it.reference) }

    firestore.collection("seat_reservations")
        .whereEqualTo("userId", userRef)
        .get().await()
        .forEach { batch.delete(it.reference) }

    batch.commit().await()
}

