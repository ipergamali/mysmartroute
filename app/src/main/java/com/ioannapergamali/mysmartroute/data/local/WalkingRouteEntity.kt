// Οντότητα Room για διαδρομή πεζών.
// Room entity for walking route.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Οντότητα που αποθηκεύει μια διαδρομή πεζών με κωδικοποιημένη polyline.
 */
@Entity(tableName = "walking_routes")
data class WalkingRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val polyline: String
)

