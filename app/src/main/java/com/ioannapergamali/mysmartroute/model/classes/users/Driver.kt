package com.ioannapergamali.mysmartroute.model.classes.users

import com.ioannapergamali.mysmartroute.model.enumerations.UserRole

open class Driver(
    id: String,
    name: String,
    email: String,
    surname: String,
    address: UserAddress,
    phoneNum: String,
    username: String
) : Passenger(id, name, email, surname, address, phoneNum, username) {
    override fun getRole() = UserRole.DRIVER
}
