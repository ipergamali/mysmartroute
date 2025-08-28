package com.ioannapergamali.mysmartroute.utils

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Χρηστικές συναρτήσεις για τον υπολογισμό χρόνου περπατήματος.
 */
object WalkingUtils {
    private const val DEFAULT_WALKING_SPEED_MPS = 1.4

    /**
     * Επιστρέφει την εκτιμώμενη διάρκεια περπατήματος για την απόσταση [distanceMeters].
     */
    fun walkingDuration(
        distanceMeters: Double,
        speedMps: Double = DEFAULT_WALKING_SPEED_MPS
    ): Duration {
        val seconds = distanceMeters / speedMps
        return seconds.toDuration(DurationUnit.SECONDS)
    }
}

