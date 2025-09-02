package com.ioannapergamali.mysmartroute.model

/**
 * Αναπαράσταση μιας πεζής μετακίνησης που αποθηκεύεται στο Firestore.
 */
data class Walk(
    val routeId: String = "",
    val userId: String = "",
    val durationMinutes: Long = 0L
)
