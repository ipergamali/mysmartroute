// Βοηθητικές λειτουργίες για όχημα.
// Helper operations for vehicle.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/**
 * Εισάγει ένα όχημα δημιουργώντας αν χρειάζεται μια ελάχιστη εγγραφή χρήστη
 * ώστε να ικανοποιείται ο περιορισμός του ξένου κλειδιού.
 */
@Transaction
suspend fun insertVehicleSafely(
    vehicleDao: VehicleDao,
    userDao: UserDao,
    vehicle: VehicleEntity
) {
    val existingUser = userDao.getUser(vehicle.userId)
    if (existingUser == null) {
        // Δημιουργούμε placeholder χρήστη με μόνο το id.
        userDao.insert(UserEntity(id = vehicle.userId))
    }
    vehicleDao.insert(vehicle)
}
