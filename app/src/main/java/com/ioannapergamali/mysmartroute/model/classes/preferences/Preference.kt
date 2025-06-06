package com.ioannapergamali.mysmartroute.model.classes.preferences

import com.ioannapergamali.mysmartroute.model.classes.routes.Route

/**
 * Stores user preferences for vehicles and routes.
 */
data class Preference(
    val user: String,
    val preferredVehicle: List<String> = emptyList(),
    val nonPreferredVehicle: List<String> = emptyList(),
    val preferredRoute: List<Route> = emptyList()
)

