package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.ioannapergamali.mysmartroute.model.enumerations.PoIType

@Entity(tableName = "pois")
data class PoIEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    @Embedded val address: PoiAddress = PoiAddress(),
    val type: PoIType = PoIType.HISTORICAL,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
