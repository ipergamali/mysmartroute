// Οντότητα Room για στάση λεωφορείου διαδρομής.
// Room entity for a route bus station.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity

@Entity(tableName = "route_bus_station", primaryKeys = ["routeId", "position"])
data class RouteBusStationEntity(
    val routeId: String,
    val position: Int,
    val poiId: String
)
