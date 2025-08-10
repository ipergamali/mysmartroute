package com.ioannapergamali.mysmartroute.data.local

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

/**
 * Υπολογίζει την [MovingStatus] μιας [MovingEntity] με βάση την κατάσταση αποδοχής
 * και τη χρονική στιγμή της μετακίνησης.
 */
fun MovingEntity.movingStatus(now: Long = System.currentTimeMillis()): MovingStatus = when {
    status == "completed" -> MovingStatus.COMPLETED
    status == "accepted" -> MovingStatus.ACTIVE
    status != "accepted" && date > now -> MovingStatus.PENDING
    else -> MovingStatus.UNSUCCESSFUL
}
