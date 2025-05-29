package com.ioannapergamali.mysmartroute.model.interfaces;

import com.ioannapergamali.mysmartroute.model.classes.users.UserAddress
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole

interface User {

    val id: String
    val name: String
    val email: String
    val surname: String
    val address: UserAddress
    val phoneNum: String
    val username: String
    val password: String

    fun getRole(): UserRole
}
