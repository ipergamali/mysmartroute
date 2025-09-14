// Οντότητα Room για μετακίνηση.
// Room entity for moving.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "movings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class MovingEntity(
    @PrimaryKey val id: String = "",
    val routeId: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val cost: Double? = null,
    val durationMinutes: Int = 0,
    /** Το όχημα που θα εκτελέσει τη μετακίνηση */
    val vehicleId: String = "",
    /** Το σημείο εκκίνησης της διαδρομής */
    val startPoiId: String = "",
    /** Το σημείο τερματισμού της διαδρομής */
    val endPoiId: String = "",
    /** Ο οδηγός που ενδιαφέρεται να πραγματοποιήσει τη μεταφορά */
    val driverId: String = "",
    /** Κατάσταση προσφοράς: open, pending, accepted, rejected, completed */
    val status: String = "open",
    /** Μοναδικός αριθμός αιτήματος */
    val requestNumber: Int = 0
) {
    @Ignore
    var createdById: String = ""

    @Ignore
    var createdByName: String = ""

    @Ignore
    var driverName: String = ""

    @Ignore
    var routeName: String = ""

    @Ignore
    var vehicleName: String = ""

    constructor(
        id: String = "",
        routeId: String = "",
        userId: String = "",
        date: Long = 0L,
        cost: Double? = null,
        durationMinutes: Int = 0,
        createdById: String = "",
        createdByName: String = "",
        driverId: String = "",
        status: String = "open",
        requestNumber: Int = 0,
        driverName: String = "",
        routeName: String = "",
        vehicleName: String = "",
        vehicleId: String = "",
        startPoiId: String = "",
        endPoiId: String = ""
    ) : this(
        id,
        routeId,
        userId,
        date,
        cost,
        durationMinutes,
        vehicleId,
        startPoiId,
        endPoiId,
        driverId,
        status,
        requestNumber
    ) {
        this.createdById = createdById
        this.createdByName = createdByName
        this.driverName = driverName
        this.routeName = routeName
        this.vehicleName = vehicleName
    }
}
