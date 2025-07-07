package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity

@Entity(tableName = "route_points", primaryKeys = ["routeId", "position"])
data class RoutePointEntity(
    val routeId: String,
    val position: Int,
    val poiId: String
)
