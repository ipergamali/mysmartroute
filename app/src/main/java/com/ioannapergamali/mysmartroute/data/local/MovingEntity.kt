package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "movings")
data class MovingEntity(
    @PrimaryKey val id: String = "",
    val routeId: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val vehicleId: String = "",
    val cost: Double = 0.0,
    val durationMinutes: Int = 0,
    /** Σημείο επιβίβασης */
    val startPoiId: String = "",
    /** Σημείο αποβίβασης */
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

    constructor(
        id: String = "",
        routeId: String = "",
        userId: String = "",
        date: Long = 0L,
        vehicleId: String = "",
        cost: Double = 0.0,
        durationMinutes: Int = 0,
        startPoiId: String = "",
        endPoiId: String = "",
        createdById: String = "",
        createdByName: String = "",
        driverId: String = "",
        status: String = "open",
        requestNumber: Int = 0,
        driverName: String = "",
        routeName: String = ""
    ) : this(
        id,
        routeId,
        userId,
        date,
        vehicleId,
        cost,
        durationMinutes,
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
    }
}
