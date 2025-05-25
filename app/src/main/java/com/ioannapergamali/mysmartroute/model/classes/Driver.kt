package com.ioannapergamali.mysmartroute.model.classes

class Driver (
    override val id: String,
    override val name: String,
    override val email: String,
    override val surname: String,
    override val address: UserAddress,
    override val phoneNum: String,
    override val username: String,
    override val password: String

) : User {
    override fun getRole() = UserRole.DRIVER
}