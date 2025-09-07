package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Οντότητα Room που συνδέει έναν χρήστη με μια διαδρομή που τον ενδιαφέρει.
 * Room entity linking a user to a route of interest.
 */
@Entity(
    tableName = "favorite_routes",
    primaryKeys = ["userId", "routeId"],
    indices = [Index("userId"), Index("routeId")]
)
data class FavoriteRouteEntity(
    val userId: String,
    val routeId: String
)
