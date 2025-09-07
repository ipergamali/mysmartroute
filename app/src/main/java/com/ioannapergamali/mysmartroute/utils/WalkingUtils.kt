package com.ioannapergamali.mysmartroute.utils

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Χρηστικές συναρτήσεις για τον υπολογισμό χρόνου περπατήματος.
 * Utility functions to estimate walking time.
 */
object WalkingUtils {
    private const val DEFAULT_WALKING_SPEED_MPS = 1.4

    /**
     * Επιστρέφει την εκτιμώμενη διάρκεια περπατήματος για την απόσταση [distanceMeters].
     * Η [distanceMeters] και η [speedMps] πρέπει να είναι θετικές τιμές.
     * Returns estimated walking [Duration] for [distanceMeters]; both
     * [distanceMeters] and [speedMps] must be positive.
     */
    fun walkingDuration(
        distanceMeters: Double,
        speedMps: Double = DEFAULT_WALKING_SPEED_MPS
    ): Duration {
        require(distanceMeters >= 0 && speedMps > 0) {
            "Η απόσταση και η ταχύτητα πρέπει να είναι θετικές"
        }
        val seconds = distanceMeters / speedMps
        return seconds.toDuration(DurationUnit.SECONDS)
    }
}

