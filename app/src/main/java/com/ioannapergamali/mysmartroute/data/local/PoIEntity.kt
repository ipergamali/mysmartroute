package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pois")
data class PoIEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val type: String,
    val lat: Double,
    val lng: Double
)
