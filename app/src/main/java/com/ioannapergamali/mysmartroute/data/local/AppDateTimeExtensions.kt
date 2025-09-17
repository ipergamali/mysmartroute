package com.ioannapergamali.mysmartroute.data.local

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Επιστρέφει την αποθηκευμένη ημερομηνία/ώρα της εφαρμογής ως [LocalDateTime]. */
suspend fun MySmartRouteDatabase.currentAppDateTime(): LocalDateTime {
    val storedMillis = appDateTimeDao().getDateTime()?.timestamp ?: System.currentTimeMillis()
    return Instant.ofEpochMilli(storedMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}
