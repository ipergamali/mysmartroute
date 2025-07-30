package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Αγαπημένο μέσο μεταφοράς για έναν χρήστη. */
@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class FavoriteEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val vehicleType: String = "",
    val preferred: Boolean = true
)
