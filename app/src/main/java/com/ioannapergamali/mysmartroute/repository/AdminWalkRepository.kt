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
     * Ανακτά όλα τα walks και υπολογίζει τη διάρκεια τους βάσει startTime/endTime.
     */
    suspend fun fetchAllWalks(): List<Walk> {
        val snapshot = db.collectionGroup("walks").get().await()
        return snapshot.documents.map { doc ->
            val start = doc.getTimestamp("startTime")
            val end = doc.getTimestamp("endTime")
            val duration = if (start != null && end != null) {
                (end.seconds - start.seconds) / 60
            } else 0L
            Walk(
                id = doc.id,
                fromPoiRef = doc.getDocumentReference("fromPoiId"),
                routeRef = doc.getDocumentReference("routeId"),
                toPoiRef = doc.getDocumentReference("toPoiId"),
                startTime = start,
                endTime = end,
                durationMinutes = duration
            )
        }
    }
}
