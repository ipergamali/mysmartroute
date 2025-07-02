package com.ioannapergamali.mysmartroute.model.classes.routes

/**
 * Represents a route between two points with an estimated cost.
 */
import com.ioannapergamali.mysmartroute.data.local.PoIEntity

data class Route(
    val start: String,
    val end: String,
    val cost: Double,
    val pois: MutableList<PoIEntity> = mutableListOf()
)

