package com.ioannapergamali.mysmartroute.model.classes.vehicles

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.interfaces.User

class Bicycle(
    vid: String,
    description: String,
    userid: String,
    owner: User
) : BaseVehicle(vid, description, userid, VehicleType.BICYCLE, 1, owner)
