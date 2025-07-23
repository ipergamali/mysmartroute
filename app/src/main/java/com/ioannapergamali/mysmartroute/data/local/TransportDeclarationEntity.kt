package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Δήλωση μεταφοράς από οδηγό. */
@Entity(tableName = "transport_declarations")
data class TransportDeclarationEntity(
    @PrimaryKey val id: String = "",
    val routeId: String = "",
    val vehicleType: String = "",
    val cost: Double = 0.0,
    val durationMinutes: Int = 0,
    /** Ημερομηνία πραγματοποίησης της διαδρομής */
    val date: Long = 0L
)
