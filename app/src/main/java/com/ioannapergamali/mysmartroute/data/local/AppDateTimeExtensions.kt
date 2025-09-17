package com.ioannapergamali.mysmartroute.data.local

import com.ioannapergamali.mysmartroute.utils.ATHENS_ZONE_ID
import java.time.Instant
import java.time.LocalDateTime

/** Επιστρέφει την αποθηκευμένη ημερομηνία/ώρα της εφαρμογής ως [LocalDateTime]. */
suspend fun MySmartRouteDatabase.currentAppDateTime(): LocalDateTime {
    val storedMillis = appDateTimeDao().getDateTime()?.timestamp ?: System.currentTimeMillis()
    return Instant.ofEpochMilli(storedMillis)
        .atZone(ATHENS_ZONE_ID)
        .toLocalDateTime()
}
