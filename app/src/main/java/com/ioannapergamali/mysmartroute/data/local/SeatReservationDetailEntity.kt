package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Υποσυλλογή για λεπτομέρειες κράτησης θέσης.
 * Αποθηκεύει τα σημεία επιβίβασης και αποβίβασης για κάθε κράτηση.
 */
@Entity(
    tableName = "seat_reservation_details",
    foreignKeys = [
        ForeignKey(
            entity = SeatReservationEntity::class,
            parentColumns = ["id"],
            childColumns = ["reservationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reservationId"])]
)
data class SeatReservationDetailEntity(
    @PrimaryKey val id: String = "",
    val reservationId: String = "",
    val startPoiId: String = "",
    val endPoiId: String = ""
)
