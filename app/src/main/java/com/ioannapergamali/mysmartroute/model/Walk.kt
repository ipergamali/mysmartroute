package com.ioannapergamali.mysmartroute.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

/**
 * Αναπαράσταση μιας πεζής μετακίνησης που αποθηκεύεται στο Firestore.
 */
data class Walk(
    val id: String = "",
    val fromPoiRef: DocumentReference? = null,
    val routeRef: DocumentReference? = null,
    val toPoiRef: DocumentReference? = null,
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val walkDurationMinutes: Long = 0L
)
