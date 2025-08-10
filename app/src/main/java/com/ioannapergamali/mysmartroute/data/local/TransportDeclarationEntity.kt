package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Δήλωση μεταφοράς από οδηγό. */
@Entity(tableName = "transport_declarations")
data class TransportDeclarationEntity(
    @PrimaryKey val id: String = "",
    val routeId: String = "",
    /** Ο οδηγός που αναλαμβάνει τη μεταφορά */
    val driverId: String = "",
    /** Το όχημα που χρησιμοποιείται */
    val vehicleId: String = "",
    val vehicleType: String = "",
    val cost: Double = 0.0,
    val durationMinutes: Int = 0,
    /** Διαθέσιμες θέσεις στο όχημα */
    val seats: Int = 0,
    /** Ημερομηνία πραγματοποίησης της διαδρομής */
    val date: Long = 0L,
    /** Ώρα έναρξης της διαδρομής σε millis από τα μεσάνυχτα */
    val startTime: Long = 0L
)
