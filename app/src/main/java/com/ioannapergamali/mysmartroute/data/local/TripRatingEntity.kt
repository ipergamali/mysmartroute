package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Πίνακας που αποθηκεύει τη βαθμολογία και το σχόλιο για ολοκληρωμένες μετακινήσεις.
 * Το movingId αντιστοιχεί στο {@link MovingEntity#id} και το userId στο {@link UserEntity#id}.
 */
@Entity(
    tableName = "trip_ratings",
    foreignKeys = [
        ForeignKey(
            entity = MovingEntity::class,
            parentColumns = ["id"],
            childColumns = ["movingId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("movingId"), Index("userId")]
)
data class TripRatingEntity(
    @PrimaryKey val movingId: String,
    val userId: String = "",
    val rating: Int = 0,
    val comment: String = "",
)
