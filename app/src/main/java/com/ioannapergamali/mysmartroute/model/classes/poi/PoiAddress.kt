package com.ioannapergamali.mysmartroute.model.classes.poi

data class PoiAddress(
    var country: String = "",
    var city: String = "",
    var streetName: String = "",
    var streetNum: Int = 0,
    var postalCode: Int = 0
)
