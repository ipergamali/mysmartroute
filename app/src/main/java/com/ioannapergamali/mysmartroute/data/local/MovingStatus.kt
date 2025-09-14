// Καταστάσεις για μετακίνηση.
// Statuses for moving.
package com.ioannapergamali.mysmartroute.data.local

import android.util.Log

/**
 * Περιγράφει την κατάσταση μιας μετακίνησης όπως εμφανίζεται στην εφαρμογή.
 *
 * - [ACTIVE]    Μετακινήσεις "accepted" που δεν έχουν ολοκληρωθεί ακόμη.
 * - [PENDING]   Μετακινήσεις "open" που ακόμη δεν έχει παρέλθει η προγραμματισμένη ώρα.
 * - [UNSUCCESSFUL] Μετακινήσεις "open" των οποίων έχει λήξει ο χρόνος χωρίς να γίνουν αποδεκτές.
 * - [COMPLETED] Μετακινήσεις με status "completed", δηλαδή όταν ο οδηγός έχει πατήσει ολοκλήρωση.
 */
enum class MovingStatus {
    ACTIVE,
    PENDING,
    UNSUCCESSFUL,
    COMPLETED
}

private const val TAG = "MovingStatus"

/**
 * Υπολογίζει την [MovingStatus] μιας [MovingEntity] με βάση την κατάσταση αποδοχής
 * και τη χρονική στιγμή της μετακίνησης.
 */
fun MovingEntity.movingStatus(now: Long = System.currentTimeMillis()): MovingStatus {
    val result = when (status.lowercase()) {
        "completed" -> MovingStatus.COMPLETED
        "accepted" -> MovingStatus.ACTIVE
        // Τα statuses "open" και "pending" αντιμετωπίζονται το ίδιο:
        // αν η ημερομηνία είναι μελλοντική θεωρούνται εκκρεμείς, αλλιώς ανεπιτυχείς.
        "open", "pending" -> if (date > now) {
            MovingStatus.PENDING
        } else {
            MovingStatus.UNSUCCESSFUL
        }
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
    val grouped = movings
        .groupBy { it.movingStatus(now) }
        .filterKeys { it != MovingStatus.ACTIVE }
    grouped.forEach { (status, list) ->

        Log.d(TAG, "Ομαδοποίηση $status -> ${list.size} εγγραφές")

    }
    return grouped
}
