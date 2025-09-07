package com.ioannapergamali.mysmartroute.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
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
     * Αποθηκεύει ή ενημερώνει ένα αγαπημένο POI.
     */
    suspend fun saveFavorite(poiId: String) {
        val uid = userId()
        if (uid.isBlank()) return
        val poiRef = firestore.collection("pois").document(poiId)
        userFavorites(uid).document(poiId).set(mapOf("poiRef" to poiRef)).await()
    }

    /**
     * Overload που δέχεται οντότητα POI.
     */
    suspend fun saveFavorite(poi: PoIEntity) = saveFavorite(poi.id)

    /**
     * Αφαιρεί ένα POI από τα αγαπημένα.
     */
    suspend fun removeFavorite(poiId: String) {
        val uid = userId()
        if (uid.isBlank()) return
        userFavorites(uid).document(poiId).delete().await()
    }

    /**
     * Επιστρέφει τις αναφορές όλων των αγαπημένων POIs.
     */
    suspend fun getFavoriteRefs(): List<DocumentReference> {
        val uid = userId()
        if (uid.isBlank()) return emptyList()
        val snap = userFavorites(uid).get().await()
        return snap.documents.mapNotNull { it.getDocumentReference("poiRef") }
    }
}

