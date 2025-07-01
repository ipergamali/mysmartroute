package com.ioannapergamali.mysmartroute.model.classes.poi

import com.ioannapergamali.mysmartroute.model.enumerations.PoIType
import com.ioannapergamali.mysmartroute.model.interfaces.PoI
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress

/**
 * Basic implementation of [PoI] used to represent a simple point of interest.
 */
data class Poi(
    override val id: String,
    override val name: String,
    override val address: PoiAddress,
    override val type: PoIType
) : PoI


