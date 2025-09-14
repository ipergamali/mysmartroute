package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Υποσυλλογή για λεπτομέρειες μετακινήσεων.
 * Αποθηκεύει τα σημεία και το όχημα για κάθε μετακίνηση.
 */
@Entity(
    tableName = "moving_details",
    foreignKeys = [
        ForeignKey(
            entity = MovingEntity::class,
            parentColumns = ["id"],
            childColumns = ["movingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["movingId"])]
)
data class MovingDetailEntity(
    @PrimaryKey val id: String = "",
    val movingId: String = "",
    val startPoiId: String = "",
    val endPoiId: String = "",
    val vehicleId: String = ""
)
