package com.ioannapergamali.mysmartroute.model.classes.users

data class UserAddress(
    var city: String = "",
    var streetName: String = "",
    var streetNum: Int = 0,
    var postalCode: Int = 0
)
