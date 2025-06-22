package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/**
 * Εισάγει όχημα μόνο εφόσον υπάρχει ο αντίστοιχος χρήστης.
 * Αν δεν υπάρχει, δημιουργείται πρώτα placeholder χρήστης με το συγκεκριμένο id.
 */
@Transaction
suspend fun insertVehicleSafely(
    vehicleDao: VehicleDao,
    userDao: UserDao,
    vehicle: VehicleEntity
) {
    val userId = vehicle.userId
    if (userDao.getUser(userId) == null) {
        userDao.insert(UserEntity(id = userId))
    }
    vehicleDao.insert(vehicle)
}
