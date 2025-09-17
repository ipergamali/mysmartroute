package com.ioannapergamali.mysmartroute.utils

import java.time.ZoneId
import java.util.TimeZone

/** Ζώνη ώρας και χρονική ζώνη που αντιστοιχούν στην Ελλάδα (Αθήνα). */
val ATHENS_ZONE_ID: ZoneId = ZoneId.of("Europe/Athens")

/** [TimeZone] για χρήση με APIs που δεν υποστηρίζουν [ZoneId]. */
val ATHENS_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Europe/Athens")
