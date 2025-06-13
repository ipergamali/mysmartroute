package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey var id: String = "",
    var description: String = "",
    var userId: String = "",
    var type: String = "",
    var seat: Int = 0
)
