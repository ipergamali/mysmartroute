package com.ioannapergamali.mysmartroute.model.classes.routes

import com.google.firebase.firestore.DocumentReference

/**
 * Μοντέλο διαδρομής που αποθηκεύεται στο Firestore με αναφορές σε PoIs.
 */
data class RouteFirestore(
    val id: String = "",
    val start: DocumentReference? = null,
    val end: DocumentReference? = null
)
