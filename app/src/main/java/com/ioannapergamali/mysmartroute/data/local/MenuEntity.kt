package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Στοιχείο μενού συνδεδεμένο με ρόλο. */
@Entity(
    tableName = "menus",
    indices = [Index("roleId")]
)
data class MenuEntity(
    @PrimaryKey var id: String = "",
    var roleId: String = "",
    var title: String = ""
)
