package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus

/** Αίτημα μεταφοράς μεταξύ επιβάτη και οδηγού. */
@Entity(tableName = "transfer_requests")
data class TransferRequestEntity(
    @PrimaryKey(autoGenerate = true) val requestNumber: Int = 0,
    val routeId: String = "",
    val passengerId: String = "",
    val driverId: String = "",
    val firebaseId: String = "",

    /** Ημερομηνία σε millis */
    val date: Long = 0L,
    val cost: Double = 0.0,
    /** Κατάσταση αιτήματος */
    val status: RequestStatus = RequestStatus.PENDING
)
