package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Τύπος σημείου ενδιαφέροντος. */
@Entity(tableName = "poi_types")
data class PoiTypeEntity(
    @PrimaryKey val id: String = "",
    val name: String = ""
)
