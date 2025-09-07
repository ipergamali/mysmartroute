package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Οντότητα Room που συνδέει έναν χρήστη με ένα αποθηκευμένο σημείο ενδιαφέροντος.
 * Room entity linking a user to a saved point of interest.
 */
@Entity(
    tableName = "user_pois",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PoIEntity::class,
            parentColumns = ["id"],
            childColumns = ["poiId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("poiId")]
)
data class UserPoiEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val poiId: String = ""
)
