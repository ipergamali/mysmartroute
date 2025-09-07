package com.ioannapergamali.mysmartroute.repository


import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import kotlinx.coroutines.tasks.await

/**
 * Repository για αποθήκευση αγαπημένων σημείων ενδιαφέροντος στο Firestore.

 * Τα αγαπημένα αποθηκεύονται στη διαδρομή `users/{uid}/Favorites/data/pois`.
 */
class FavoritesRepository {
    private val firestore = Firebase.firestore

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private fun userFavorites(uid: String) = firestore
        .collection("users")
        .document(uid)
        .collection("Favorites")
        .document("data")
        .collection("pois")

    /**
     * Αποθηκεύει ή ενημερώνει ένα αγαπημένο POI με αναφορά στο έγγραφο του.
     */
    suspend fun saveFavorite(poi: PoIEntity) {
        val uid = userId()
        if (uid.isBlank()) return
        val poiRef = firestore.collection("pois").document(poi.id)
        userFavorites(uid).document(poi.id).set(mapOf("poiRef" to poiRef)).await()

    }
}

