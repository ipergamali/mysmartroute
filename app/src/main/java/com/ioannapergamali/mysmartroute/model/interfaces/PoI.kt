package com.ioannapergamali.mysmartroute.model.interfaces

import com.ioannapergamali.mysmartroute.model.enumerations.PoIType
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress

interface PoI {
    val id: String
    val address: PoiAddress
    val type: PoIType
}
