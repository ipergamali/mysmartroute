package com.ioannapergamali.mysmartroute.model.classes.vehicles

/**
 * Απλό μοντέλο για όχημα που επιστρέφεται από το Google Places API.
 */
data class RemoteVehicle(
    val name: String,
    val address: String?
)
