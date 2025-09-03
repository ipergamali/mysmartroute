package com.ioannapergamali.mysmartroute.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.ioannapergamali.mysmartroute.model.Walk

/**
 * Repository για συλλογή όλων των πεζών μετακινήσεων από όλα τα `walks` subcollections.
 */
class AdminWalkRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Ανακτά όλα τα walks και διαβάζει το αποθηκευμένο `walkDurationMinutes`.
     */
    suspend fun fetchAllWalks(): List<Walk> {
        val snapshot = db.collectionGroup("walks").get().await()
        return snapshot.documents.map { doc ->
            val start = doc.getTimestamp("startTime")
            val end = doc.getTimestamp("endTime")
            val duration = doc.getLong("walkDurationMinutes") ?: 0L
            Walk(
                id = doc.id,
                routeRef = doc.getDocumentReference("routeId"),
                startTime = start,
                endTime = end,
                walkDurationMinutes = duration
            )
        }
    }
}
