package com.ioannapergamali.mysmartroute.model.classes;
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
