package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String = "",
    val startPoiId: String = "",
    val endPoiId: String = "",
    val cost: Double = 0.0
)
