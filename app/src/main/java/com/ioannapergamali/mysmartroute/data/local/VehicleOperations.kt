// Βοηθητικές λειτουργίες για όχημα.
// Helper operations for vehicle.
package com.ioannapergamali.mysmartroute.data.local

import android.util.Log
import androidx.room.withTransaction

/**
 * Εισάγει ένα όχημα δημιουργώντας αν χρειάζεται μια ελάχιστη εγγραφή χρήστη
 * ώστε να ικανοποιείται ο περιορισμός του ξένου κλειδιού.
 */
private const val TAG = "VehicleOps"

suspend fun insertVehicleSafely(
    db: MySmartRouteDatabase,
    vehicle: VehicleEntity
) {
    db.withTransaction {
        val userDao = db.userDao()
        val vehicleDao = db.vehicleDao()

        val existingUser = userDao.getUser(vehicle.userId)
        if (existingUser == null) {
            Log.d(TAG, "Δημιουργία προσωρινού χρήστη ${vehicle.userId}")
            // Δημιουργούμε placeholder χρήστη με μόνο το id.
            userDao.insert(UserEntity(id = vehicle.userId))
        } else {
            Log.d(TAG, "Ο χρήστης ${vehicle.userId} υπάρχει ήδη")
        }
        vehicleDao.insert(vehicle)
        Log.d(TAG, "Εισαγωγή οχήματος ${vehicle.id} για χρήστη ${vehicle.userId}")
    }
}
