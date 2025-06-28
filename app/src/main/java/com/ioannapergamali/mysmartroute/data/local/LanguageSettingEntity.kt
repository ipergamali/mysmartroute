package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_language")
data class LanguageSettingEntity(
    @PrimaryKey val id: Int = 1,
    var language: String = "el"
)
