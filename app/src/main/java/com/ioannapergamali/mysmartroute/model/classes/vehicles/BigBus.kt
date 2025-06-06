package com.ioannapergamali.mysmartroute.model.classes.vehicles

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.interfaces.User

class BigBus(
    vid: String,
    description: String,
    userid: String,
    seat: Int,
    owner: User
) : BaseVehicle(vid, description, userid, VehicleType.BIGBUS, seat, owner)
