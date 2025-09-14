package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/** Λεπτομέρεια δήλωσης μεταφοράς με συσχέτιση οχήματος και σημείων. */
@Entity(
    tableName = "transport_declarations_details",
    foreignKeys = [
        ForeignKey(
            entity = TransportDeclarationEntity::class,
            parentColumns = ["id"],
            childColumns = ["declarationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["declarationId"])]
)
data class TransportDeclarationDetailEntity(
    @PrimaryKey val id: String = "",
    val declarationId: String = "",
    val startPoiId: String = "",
    val endPoiId: String = "",
    val vehicleId: String = "",
    val vehicleType: String = "",
    val seats: Int = 0,
    val startTime: Long = 0L
)
