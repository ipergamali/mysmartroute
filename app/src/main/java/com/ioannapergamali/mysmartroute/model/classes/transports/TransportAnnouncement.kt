package com.ioannapergamali.mysmartroute.model.classes.transports

import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

/**
 * Represents a driver's availability for a specific transport.
 */
data class TransportAnnouncement(
    val id: String,
    val driverId: String,
    val vehicleType: VehicleType,
    val route: Route,
    val date: Int,
    val cost: Double,
    val durationMinutes: Int
)
