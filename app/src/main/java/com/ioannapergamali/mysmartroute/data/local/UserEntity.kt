package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var surname: String = "",
    var username: String = "",
    var email: String = "",
    var phoneNum: String = "",
    var photoUrl: String? = null,
    var password: String = "",
    var role: String = "",
    var roleId: String = "",
    var city: String = "",
    var streetName: String = "",
    var streetNum: Int = 0,
    var postalCode: Int = 0
)
