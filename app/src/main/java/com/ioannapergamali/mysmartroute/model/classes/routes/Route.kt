package com.ioannapergamali.mysmartroute.model.classes.routes

/**
 * Αναπαράσταση διαδρομής μεταξύ δύο σημείων.
 */
import com.ioannapergamali.mysmartroute.data.local.PoIEntity

data class Route(
    val name: String = "",
    val start: String,
    val end: String,
    val pois: MutableList<PoIEntity> = mutableListOf()
)

