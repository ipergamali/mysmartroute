package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authentication")
data class AuthenticationEntity(
    @PrimaryKey val id: String = ""
)
