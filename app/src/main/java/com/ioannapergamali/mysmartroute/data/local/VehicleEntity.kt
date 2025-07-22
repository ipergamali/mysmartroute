package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vehicles",
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
data class VehicleEntity(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var description: String = "",
    var userId: String = "",
    var type: String = "",
    var seat: Int = 0,
    var color: String = "",
    var plate: String = ""
)
