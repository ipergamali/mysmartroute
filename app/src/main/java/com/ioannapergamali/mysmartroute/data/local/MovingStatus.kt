// Καταστάσεις για μετακίνηση.
// Statuses for moving.
package com.ioannapergamali.mysmartroute.data.local

import android.util.Log

/**
 * Περιγράφει την κατάσταση μιας μετακίνησης όπως εμφανίζεται στην εφαρμογή.
 *
 * - [PENDING]   Μετακινήσεις "open", "pending" ή "accepted" πριν την προγραμματισμένη ώρα.
 * - [UNSUCCESSFUL] Μετακινήσεις που δεν ολοκληρώθηκαν εγκαίρως ή απορρίφθηκαν/ακυρώθηκαν.
 * - [COMPLETED] Μετακινήσεις με status "completed", δηλαδή όταν ο οδηγός έχει πατήσει ολοκλήρωση.
 */
enum class MovingStatus {
    PENDING,
    UNSUCCESSFUL,
    COMPLETED
}

private const val TAG = "MovingStatus"
private val ACTIVE_STATUSES = setOf("open", "pending", "accepted")
private val CANCELED_STATUSES = setOf("rejected", "canceled", "cancelled")

/**
 * Υπολογίζει την [MovingStatus] μιας [MovingEntity] με βάση την κατάσταση αποδοχής
 * και τη χρονική στιγμή της μετακίνησης.
 */
fun MovingEntity.movingStatus(now: Long = System.currentTimeMillis()): MovingStatus {
    val normalizedStatus = status.trim().lowercase()
    val result = when {
        normalizedStatus == "completed" -> MovingStatus.COMPLETED
        normalizedStatus.isEmpty() || normalizedStatus in ACTIVE_STATUSES ->
            if (date > now) {
                MovingStatus.PENDING
            } else {
                MovingStatus.UNSUCCESSFUL
            }
        normalizedStatus in CANCELED_STATUSES -> MovingStatus.UNSUCCESSFUL
        date > now -> MovingStatus.PENDING
        else -> MovingStatus.UNSUCCESSFUL
    }

    Log.d(TAG, "Μετακίνηση $id με raw status '$status' ταξινομήθηκε ως $result")

    return result
}

/**
 * Ομαδοποιεί μια λίστα από [MovingEntity] ανά [MovingStatus].
 */
fun categorizeMovings(
    movings: List<MovingEntity>,
    now: Long = System.currentTimeMillis()
): Map<MovingStatus, List<MovingEntity>> {
    val grouped = movings.groupBy { it.movingStatus(now) }
    grouped.forEach { (status, list) ->

        Log.d(TAG, "Ομαδοποίηση $status -> ${list.size} εγγραφές")

    }
    return grouped
}
