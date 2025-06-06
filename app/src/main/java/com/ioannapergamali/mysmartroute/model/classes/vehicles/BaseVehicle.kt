package com.ioannapergamali.mysmartroute.model.classes.vehicles

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.interfaces.User
import com.ioannapergamali.mysmartroute.model.interfaces.Vehicle

open class BaseVehicle(
    override val vid: String,
    override val description: String,
    override val userid: String,
    private val vehicleType: VehicleType,
    override var seat: Int,
    private val owner: User
) : Vehicle {
    override var type: String = vehicleType.name

    override fun getType(): VehicleType = vehicleType

    override fun getownership(): User = owner
}
