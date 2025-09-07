// Οντότητα Room για πεζή μετακίνηση.
// Room entity for walking.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Οντότητα που αναπαριστά μια πεζή διαδρομή που έχει αποθηκευτεί από τον χρήστη.
 */
@Entity(tableName = "walking")
data class WalkingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val routeId: String,
    val startTime: Long
)
