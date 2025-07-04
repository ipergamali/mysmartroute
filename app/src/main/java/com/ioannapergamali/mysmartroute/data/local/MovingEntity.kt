package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movings")
data class MovingEntity(
    @PrimaryKey val id: String = "",
    val routeId: String = "",
    val userId: String = "",
    val date: Int = 0,
    val vehicleId: String = "",
    val cost: Double = 0.0,
    val durationMinutes: Int = 0
)
