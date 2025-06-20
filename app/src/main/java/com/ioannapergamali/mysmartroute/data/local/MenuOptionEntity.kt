package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Επιλογή ενός μενού. */
@Entity(
    tableName = "menu_options",
    indices = [Index("menuId")]
)
data class MenuOptionEntity(
    @PrimaryKey var id: String = "",
    var menuId: String = "",
    var title: String = "",
    var route: String = ""
)
