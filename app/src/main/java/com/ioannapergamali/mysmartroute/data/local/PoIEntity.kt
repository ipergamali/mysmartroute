package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pois")
data class PoIEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val country: String = "",
    val city: String = "",
    val streetName: String = "",
    val streetNum: Int = 0,
    val postalCode: Int = 0,
    val type: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
