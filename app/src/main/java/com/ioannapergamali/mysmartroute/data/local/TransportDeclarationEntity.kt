// Οντότητα Room για δήλωση μεταφοράς.
// Room entity for transport declaration.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

/** Δήλωση μεταφοράς από οδηγό. */
@Entity(tableName = "transport_declarations")
data class TransportDeclarationEntity(
    @PrimaryKey val id: String = "",
    val routeId: String = "",
    /** Ο οδηγός που αναλαμβάνει τη μεταφορά */
    val driverId: String = "",
    val cost: Double = 0.0,
    val durationMinutes: Int = 0,
    /** Διαθέσιμες θέσεις στο όχημα */
    val seats: Int = 0,
    /** Ημερομηνία πραγματοποίησης της διαδρομής */
    val date: Long = 0L,
    /** Ώρα έναρξης της διαδρομής σε millis από τα μεσάνυχτα */
    val startTime: Long = 0L
) {
    /** Συμβατότητα με παλαιό κώδικα: τα πεδία οχήματος δεν αποθηκεύονται πλέον στον πίνακα. */
    @Ignore var vehicleId: String = ""
    @Ignore var vehicleType: String = ""

    constructor(
        id: String = "",
        routeId: String = "",
        driverId: String = "",
        vehicleId: String = "",
        vehicleType: String = "",
        cost: Double = 0.0,
        durationMinutes: Int = 0,
        seats: Int = 0,
        date: Long = 0L,
        startTime: Long = 0L
    ) : this(id, routeId, driverId, cost, durationMinutes, seats, date, startTime) {
        this.vehicleId = vehicleId
        this.vehicleType = vehicleType
    }
}
