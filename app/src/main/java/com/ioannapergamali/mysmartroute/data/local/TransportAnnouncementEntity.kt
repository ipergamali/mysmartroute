package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transport_announcements")
data class TransportAnnouncementEntity(
    @PrimaryKey val id: String = "",
    val driverId: String = "",
    val vehicleType: String = "",
    val start: String = "",
    val end: String = "",
    val date: Int = 0,
    val cost: Double = 0.0,
    val durationMinutes: Int = 0
)
