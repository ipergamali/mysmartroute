package com.ioannapergamali.mysmartroute.model.interfaces

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

interface Vehicle {
    val vid: String
    val description: String
    val userid:String
    var type:String
    var seat:Int

    fun getType(): VehicleType
    fun getownership():User
}