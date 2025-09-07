package com.ioannapergamali.mysmartroute.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.ioannapergamali.mysmartroute.model.Walk

/**
 * Repository για συλλογή όλων των πεζών μετακινήσεων από όλα τα `walks` subcollections.
 * Repository that collects all walking entries from every `walks` subcollection.
 */
class AdminWalkRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Ανακτά όλες τις πεζές μετακινήσεις από τα subcollections `walks` των διαδρομών.
     * Κάθε έγγραφο περιέχει μόνο τη διάρκεια σε λεπτά.
     *
     * Fetches all walking sessions from route `walks` subcollections.
     * Each document holds only the duration in minutes.
     */
    suspend fun fetchAllWalks(): List<Walk> {
        val snapshot = db.collectionGroup("walks").get().await()
        return snapshot.documents
            .filter { it.reference.parent.parent?.parent?.id == "routes" }
            .map { doc ->
                val duration = doc.getLong("durationMinutes") ?: 0L
                Walk(
                    id = doc.id,
                    durationMinutes = duration
                )
            }
    }
}
