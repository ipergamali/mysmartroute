package com.ioannapergamali.mysmartroute.model.classes.poi

import com.ioannapergamali.mysmartroute.model.enumerations.PoIType
import com.ioannapergamali.mysmartroute.model.interfaces.PoI

/**
 * Basic implementation of [PoI] used to represent a simple point of interest.
 */
data class Poi(
    override val id: String,
    override val description: String,
    override val type: PoIType
) : PoI


