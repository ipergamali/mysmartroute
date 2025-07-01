package com.ioannapergamali.mysmartroute.model.classes.transports

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

/**
 * Ενέργειες επιβάτη πάνω σε μια [ServedMoving].
 */
class PassengerServedMoving(
    private val passengerId: String,
    private val servedMoving: ServedMoving
) {
    /** Αγορά εισιτηρίου εφόσον υπάρχει διαθέσιμη θέση. */
    fun buyTicket(capacity: Int): Boolean {
        return if (servedMoving.hasAvailableSeat(capacity)) {
            servedMoving.addPassenger(passengerId)
            true
        } else {
            false
        }
    }

    /** Ακύρωση εισιτηρίου. */
    fun cancelTicket() {
        servedMoving.removePassenger(passengerId)
    }

    companion object {
        /** Υπολογίζει την διάρκεια με βάση το όχημα. */
        fun calculateDuration(type: VehicleType, duration: Int): Int = when (type) {
            VehicleType.BICYCLE -> (duration * 0.8).toInt()
            VehicleType.MOTORBIKE -> (duration * 0.4).toInt()
            VehicleType.CAR, VehicleType.TAXI -> (duration * 0.3).toInt()
            VehicleType.SMALLBUS, VehicleType.BIGBUS -> (duration * 0.6).toInt()
        }
    }
}
