package com.ioannapergamali.mysmartroute.model.classes.routes

import com.ioannapergamali.mysmartroute.model.classes.poi.Poi

/**
 * Represents a route between two points with an estimated cost.
 */
data class Route(
    val start: Poi,
    val end: Poi,
    val intermediatePois: List<Poi> = emptyList(),
    val cost: Double
)

