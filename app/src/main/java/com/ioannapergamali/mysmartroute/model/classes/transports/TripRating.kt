package com.ioannapergamali.mysmartroute.model.classes.transports

/**
 * Απλή κλάση δεδομένων για αποθήκευση βαθμολογίας ταξιδιού στο Firestore.
 */
data class TripRating(
    val movingId: String = "",
    val userId: String = "",
    val rating: Int = 0,
    val comment: String? = null
)
