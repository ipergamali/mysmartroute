package com.ioannapergamali.mysmartroute.model.classes.transports

import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus

/** Απλό μοντέλο αιτήματος μεταφοράς. */
data class TransferRequest(
    val requestNumber: Int,
    val routeId: String,
    val passengerId: String,
    val driverId: String,
    val date: Long,
    val cost: Double,
    val status: RequestStatus
)
