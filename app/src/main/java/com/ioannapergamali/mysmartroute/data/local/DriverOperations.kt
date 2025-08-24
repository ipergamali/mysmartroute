package com.ioannapergamali.mysmartroute.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Καθαρίζει όλα τα δεδομένα ενός οδηγού μόνο από τη βάση Room.
 */
suspend fun demoteDriverToPassenger(
    db: MySmartRouteDatabase,
    driverId: String,
) = withContext(Dispatchers.IO) {
    // Τοπική βάση
    db.vehicleDao().deleteForUser(driverId)
    db.transportDeclarationDao().deleteForDriver(driverId)
    db.transferRequestDao().deleteForDriver(driverId)
    db.movingDao().deleteForDriver(driverId)
}

/**
 * Καθαρίζει τα εκκρεμή δεδομένα ενός επιβάτη όταν προάγεται σε οδηγό,
 * χρησιμοποιώντας μόνο τη βάση Room.
 */
suspend fun promotePassengerToDriver(
    db: MySmartRouteDatabase,
    passengerId: String,
) = withContext(Dispatchers.IO) {
    // Τοπική βάση
    db.transferRequestDao().deleteForPassenger(passengerId)
    db.movingDao().deleteForUser(passengerId)
    db.seatReservationDao().deleteForUser(passengerId)
}

