package com.ioannapergamali.mysmartroute.model.classes.transports

import com.ioannapergamali.mysmartroute.data.local.MovingEntity

/**
 * Συνδυάζει μια ολοκληρωμένη μετακίνηση με τη βαθμολογία και το σχόλιό της.
 */
data class TripWithRating(
    val moving: MovingEntity,
    val rating: Int = 0,
    val comment: String = ""
)
