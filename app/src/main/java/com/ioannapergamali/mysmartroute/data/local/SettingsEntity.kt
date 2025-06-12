package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "settings",
    foreignKeys = [ForeignKey(
        entity = AuthenticationEntity::class,
        parentColumns = ["uid"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SettingsEntity(
    @PrimaryKey var userId: String = "",
    var theme: String = "",
    var darkTheme: Boolean = false,
    var font: String = "",
    var soundEnabled: Boolean = false,
    var soundVolume: Float = 0f
)
