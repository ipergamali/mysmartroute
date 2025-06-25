package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Επιλογή ενός μενού. */
@Entity(
    tableName = "menu_options",
    foreignKeys = [
        ForeignKey(
            entity = MenuEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("menuId")]
)
data class MenuOptionEntity(
    @PrimaryKey var id: String = "",
    var menuId: String = "",
    /** Κλειδί πόρου για τον τίτλο της επιλογής */
    var titleResKey: String = "",
    var route: String = ""
)
