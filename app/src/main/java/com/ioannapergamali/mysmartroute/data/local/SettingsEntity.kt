package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settings",
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
data class SettingsEntity(
    @PrimaryKey var userId: String = "",
    var theme: String = "",
    var darkTheme: Boolean = false,
    var font: String = "",
    var soundEnabled: Boolean = false,
    var soundVolume: Float = 0f,
    var language: String = "el"
)
