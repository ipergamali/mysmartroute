package com.ioannapergamali.mysmartroute.utils

import com.ioannapergamali.mysmartroute.data.local.SeatReservationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Δημιουργεί κείμενο για εκτύπωση των κρατήσεων.
 * Builds printable text of seat reservations.
 */
class ReservationPrinter(private val dao: SeatReservationDao) {
    suspend fun buildPrintText(): String = withContext(Dispatchers.IO) {
        val reservations = dao.getAllList()
        if (reservations.isEmpty()) {
            "Δεν βρέθηκαν κρατήσεις."
        } else {
            buildString {
                reservations.forEach { r ->
                    append("Διαδρομή: ${r.routeId} - Χρήστης: ${r.userId}\n")
                }
            }
        }
    }
}
