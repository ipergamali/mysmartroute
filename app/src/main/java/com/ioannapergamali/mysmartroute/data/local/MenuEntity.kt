package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Στοιχείο μενού συνδεδεμένο με ρόλο. */
@Entity(
    tableName = "menus",
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roleId")]
)
data class MenuEntity(
    @PrimaryKey var id: String = "",
    var roleId: String = "",
    /** Κλειδί πόρου για τον τίτλο του μενού */
    var titleResKey: String = ""
)
