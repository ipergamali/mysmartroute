package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Αποθηκεύει τις διαδρομές πεζή που δηλώνει ο επιβάτης
 */
@Entity(tableName = "walking")
data class WalkingRouteEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val routeId: String = "",
    val fromPoiId: String = "",
    val toPoiId: String = "",
    val date: Long = 0L
)
