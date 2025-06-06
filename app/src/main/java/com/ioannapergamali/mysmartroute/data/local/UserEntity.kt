package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val surname: String,
    val username: String,
    val email: String,
    val phoneNum: String,
    val password: String,
    val role: String,
    val city: String,
    val streetName: String,
    val streetNum: Int,
    val postalCode: Int
)
