package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Δήλωση διαθεσιμότητας οδηγού. */
@Entity(tableName = "availabilities")
data class AvailabilityEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val fromTime: Int = 0,
    val toTime: Int = 0
)
