package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Πίνακας που αποθηκεύει τη βαθμολογία και το σχόλιο για ολοκληρωμένες μετακινήσεις.
 * Το movingId αντιστοιχεί στο {@link MovingEntity#id}.
 */
@Entity(tableName = "trip_ratings")
data class TripRatingEntity(
    @PrimaryKey val movingId: String,
    val rating: Int = 0,
    val comment: String = ""
)
