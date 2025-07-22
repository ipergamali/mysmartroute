package com.ioannapergamali.mysmartroute.model.classes.transports

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

/**
 * Περιγραφή μετακίνησης που εξυπηρετείται από οδηγό.
 */
data class ServedMoving(
    val driverId: String,
    val moving: Moving,
    val durationMinutes: Int,
    val vehicleId: String,
    val passengers: MutableList<String> = mutableListOf()
) {
    /** Ελέγχει αν υπάρχει διαθέσιμη θέση στο όχημα. */
    fun hasAvailableSeat(capacity: Int): Boolean = passengers.size < capacity

    /** Προσθέτει επιβάτη εφόσον δεν υπάρχει ήδη. */
    fun addPassenger(userId: String): Boolean {
        return if (!passengers.contains(userId)) {
            passengers.add(userId)
            true
        } else {
            false
        }
    }

    /** Αφαιρεί επιβάτη από τη λίστα. */
    fun removePassenger(userId: String): Boolean = passengers.remove(userId)

    companion object {
        /** Υπολογίζει τον χρόνο περπατήματος με βάση το είδος οχήματος. */
        fun calculateWalkDuration(type: VehicleType, duration: Int): Int = when (type) {
            VehicleType.BICYCLE -> (duration * 1.25).toInt()
            VehicleType.MOTORBIKE -> (duration * 2.1).toInt()
            VehicleType.CAR, VehicleType.TAXI -> duration * 3
            VehicleType.SMALLBUS, VehicleType.BIGBUS -> (duration * 1.5).toInt()
        }
    }
}
