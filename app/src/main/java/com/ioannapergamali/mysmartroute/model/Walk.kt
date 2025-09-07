package com.ioannapergamali.mysmartroute.model

/**
 * Αναπαράσταση μιας πεζής μετακίνησης που αποθηκεύεται στο Firestore.
 * Περιέχει μόνο το αναγνωριστικό και τη διάρκεια σε λεπτά.
 *
 * Representation of a walking trip stored in Firestore.
 * Contains only the identifier and duration in minutes.
 */
data class Walk(
    val id: String = "",
    val durationMinutes: Long = 0L
)
