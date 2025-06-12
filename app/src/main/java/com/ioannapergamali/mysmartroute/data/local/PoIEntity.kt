package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "pois",
    foreignKeys = [ForeignKey(
        entity = AuthenticationEntity::class,
        parentColumns = ["uid"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PoIEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val userId: String = ""
)
