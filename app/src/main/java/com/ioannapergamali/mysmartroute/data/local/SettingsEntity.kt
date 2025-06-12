package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val userId: String,
    val theme: String,
    val darkTheme: Boolean,
    val font: String,
    val soundEnabled: Boolean,
    val soundVolume: Float
)
