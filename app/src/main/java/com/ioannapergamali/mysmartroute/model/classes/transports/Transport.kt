package com.ioannapergamali.mysmartroute.model.classes.transports

import com.ioannapergamali.mysmartroute.model.classes.routes.Route

/**
 * Basic transport information holding a route and scheduling data.
 */
data class Transport(
    val id: String,
    val username: String,
    val date: Int,
    val route: Route
)

