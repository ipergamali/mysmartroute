package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Εγγραφή κράτησης θέσης για έναν χρήστη και μια διαδρομή. */
@Entity(tableName = "seat_reservations")
data class SeatReservationEntity(
    @PrimaryKey val id: String = "",
    /** Δήλωση μεταφοράς στην οποία ανήκει η θέση */
    val declarationId: String = "",
    val routeId: String = "",
    val userId: String = "",
    /** Ημερομηνία κράτησης σε millis */
    val date: Long = 0L,
    /** Ώρα έναρξης της διαδρομής σε millis από τα μεσάνυχτα */
    val startTime: Long = 0L,
    /** Σημείο επιβίβασης */
    val startPoiId: String = "",
    /** Σημείο αποβίβασης */
    val endPoiId: String = ""
)
