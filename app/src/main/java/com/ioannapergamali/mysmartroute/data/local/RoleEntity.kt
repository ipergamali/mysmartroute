package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Οντότητα ρόλου χρήστη. */
@Entity(
    tableName = "roles",
    indices = [Index("userId")]
)
data class RoleEntity(
    @PrimaryKey var id: String = "",
    var userId: String = "",
    var name: String = ""
)
