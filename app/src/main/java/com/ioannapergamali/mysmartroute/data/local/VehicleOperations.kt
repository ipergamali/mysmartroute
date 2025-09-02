package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/**
 * Εισάγει όχημα μόνο αν υπάρχει ήδη ο αντίστοιχος χρήστης.
 * Δεν δημιουργεί πλέον placeholder εγγραφή χρήστη.
 */
@Transaction
suspend fun insertVehicleSafely(
    vehicleDao: VehicleDao,
    userDao: UserDao,
    vehicle: VehicleEntity
) {
    if (userDao.getUser(vehicle.userId) != null) {
        vehicleDao.insert(vehicle)
    }
}
