package com.ioannapergamali.mysmartroute.model.classes.vehicles

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.interfaces.User

class Taxi(
    vid: String,
    description: String,
    userid: String,
    owner: User
) : BaseVehicle(vid, description, userid, VehicleType.TAXI, 4, owner)
