package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settings",
    indices = [Index("userId")]
)
data class SettingsEntity(
    @PrimaryKey var userId: String = "",
    var theme: String = "",
    var darkTheme: Boolean = false,
    var font: String = "",
    var soundEnabled: Boolean = false,
    var soundVolume: Float = 0f
)
