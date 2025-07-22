package com.ioannapergamali.mysmartroute.model.classes.transports

import com.ioannapergamali.mysmartroute.model.classes.routes.Route

/**
 * Απλή αναπαράσταση μετακίνησης με διαδρομή, ημερομηνία και κόστος.
 */
data class Moving(
    val id: String,
    val route: Route,
    val date: Int,
    val cost: Double
)
