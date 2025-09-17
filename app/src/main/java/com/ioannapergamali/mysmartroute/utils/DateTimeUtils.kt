package com.ioannapergamali.mysmartroute.utils

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val ATHENS_ZONE_ID: ZoneId = ZoneId.of("Europe/Athens")

fun combineDateAndTimeAsAthensInstant(dateMillis: Long, timeMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(dateMillis).atZone(ATHENS_ZONE_ID).toLocalDate()
    val localTime = LocalTime.ofNanoOfDay(timeMillis * 1_000_000L)
    return ZonedDateTime.of(localDate, localTime, ATHENS_ZONE_ID).toInstant().toEpochMilli()
}
