package com.ioannapergamali.mysmartroute.data.local

enum class MovingStatus {
    ACTIVE,
    PENDING,
    UNSUCCESSFUL,
    COMPLETED
}

fun MovingEntity.movingStatus(now: Long = System.currentTimeMillis()): MovingStatus = when {
    status == "accepted" && date > now -> MovingStatus.ACTIVE
    status == "accepted" && date <= now -> MovingStatus.COMPLETED
    status != "accepted" && date > now -> MovingStatus.PENDING
    else -> MovingStatus.UNSUCCESSFUL
}
