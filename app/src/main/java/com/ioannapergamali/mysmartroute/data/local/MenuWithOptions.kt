package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Embedded
import androidx.room.Relation

/** Σχέση μενού με τις επιλογές του. */
data class MenuWithOptions(
    @Embedded val menu: MenuEntity,
    @Relation(parentColumn = "id", entityColumn = "menuId")
    val options: List<MenuOptionEntity>
)
