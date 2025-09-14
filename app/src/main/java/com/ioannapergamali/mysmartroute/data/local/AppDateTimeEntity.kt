package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Ημερομηνία/ώρα εφαρμογής. */
@Entity(tableName = "app_datetime")
data class AppDateTimeEntity(
    @PrimaryKey val id: Int = 1,
    val timestamp: Long
)
