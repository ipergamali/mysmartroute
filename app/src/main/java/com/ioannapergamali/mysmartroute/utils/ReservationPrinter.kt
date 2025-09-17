package com.ioannapergamali.mysmartroute.utils

import com.ioannapergamali.mysmartroute.data.local.SeatReservationDao
import com.ioannapergamali.mysmartroute.data.local.SeatReservationDetailDao
import com.ioannapergamali.mysmartroute.data.local.PoIDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Δημιουργεί κείμενο για εκτύπωση των κρατήσεων.
 * Builds printable text of seat reservations.
 */
class ReservationPrinter(
    private val dao: SeatReservationDao,
    private val detailDao: SeatReservationDetailDao,
    private val poiDao: PoIDao
) {
    suspend fun buildPrintText(): String = withContext(Dispatchers.IO) {
        val reservations = dao.getAllList()
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = ATHENS_TIME_ZONE
        }
        if (reservations.isEmpty()) {
            "Δεν βρέθηκαν κρατήσεις."
        } else {
            buildString {
                reservations.forEach { r ->
                    append("Διαδρομή: ${r.routeId} - Χρήστης: ${r.userId}\n")
                    val details = detailDao.getForReservation(r.id).first()
                    details.forEach { d ->
                        val start = poiDao.findById(d.startPoiId)?.name ?: d.startPoiId
                        val end = poiDao.findById(d.endPoiId)?.name ?: d.endPoiId
                        val time = formatter.format(Date(d.startTime))
                        append("  Εκκίνηση: $start - Αποβίβαση: $end - Ώρα: $time - Κόστος: ${d.cost}\n")
                    }
                }
            }
        }
    }
}
