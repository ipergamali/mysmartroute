package com.ioannapergamali.mysmartroute.model

import com.google.firebase.Timestamp

/**
 * Αναπαράσταση μιας πεζής μετακίνησης που αποθηκεύεται στο Firestore.
 */
data class Walk(
    val id: String = "",
    val userId: String = "",
    val routeId: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val durationMinutes: Long = 0L
)
