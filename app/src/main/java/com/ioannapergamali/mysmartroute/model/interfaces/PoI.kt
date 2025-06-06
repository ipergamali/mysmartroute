package com.ioannapergamali.mysmartroute.model.interfaces

import com.ioannapergamali.mysmartroute.model.enumerations.PoIType

interface PoI {
    val id: String
    val description: String
    val type: PoIType

    fun getType(): PoIType
}
