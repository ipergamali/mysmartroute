package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Εγγραφή κράτησης θέσης για έναν χρήστη και μια διαδρομή. */
@Entity(tableName = "seat_reservations")
data class SeatReservationEntity(
    @PrimaryKey val id: String = "",
    val routeId: String = "",
    val userId: String = ""
)
