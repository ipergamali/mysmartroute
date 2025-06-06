package com.ioannapergamali.mysmartroute.model.classes.routes

/**
 * Represents a route between two points with an estimated cost.
 */
data class Route(
    val start: String,
    val end: String,
    val cost: Double
)

