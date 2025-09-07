package com.ioannapergamali.mysmartroute.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import kotlinx.coroutines.tasks.await

/**
 * Repository για αποθήκευση αγαπημένων σημείων ενδιαφέροντος στο Firestore.
 * Repository for storing favorite points of interest in Firestore.
 */
class FavoritesRepository {
    private val favoritesRef = Firebase.firestore
        .collection("Favorites")
        .document("data")
        .collection("pois")

    /**
     * Αποθηκεύει ή ενημερώνει ένα αγαπημένο POI με αναφορά στο έγγραφο του.
     * Saves or updates a favorite POI referencing its document.
     */
    suspend fun saveFavorite(poi: PoIEntity) {
        val poiRef = Firebase.firestore.collection("pois").document(poi.id)
        favoritesRef.document(poi.id).set(mapOf("poiRef" to poiRef)).await()
    }
}

