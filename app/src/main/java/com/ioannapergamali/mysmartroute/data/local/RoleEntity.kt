package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Οντότητα ρόλου χρήστη. */
@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var parentRoleId: String? = null
)
