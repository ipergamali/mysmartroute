package com.ioannapergamali.mysmartroute.model.classes.users

import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.model.interfaces.User

open class Passenger(
    override val id: String,
    override val name: String,
    override val email: String,
    override val surname: String,
    override val address: UserAddress,
    override val phoneNum: String,
    override val username: String,
    override val password: String
) : User {
    override fun getRole() = UserRole.PASSENGER
}
