package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ViewModel για τη διαχείριση αγαπημένων σημείων ενδιαφέροντος (POIs).
 * Τα αγαπημένα αποθηκεύονται στη διαδρομή
 * `users/{uid}/Favorites/data/pois/{poiId}` ως αναφορά
 * στο πραγματικό έγγραφο του POI.
 */
class FavoritePoisViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private fun userPois(uid: String) = firestore
        .collection("users")
        .document(uid)
        .collection("Favorites")
        .document("data")
        .collection("pois")

    /**
     * Προσθέτει ένα POI στα αγαπημένα του χρήστη.
     */
    suspend fun addFavoritePoi(poiId: String) {
        val uid = userId()
        if (uid.isBlank()) return
        val poiRef = firestore.collection("pois").document(poiId)
        userPois(uid).document(poiId).set(mapOf("poiRef" to poiRef)).await()
    }

    /**
     * Αφαιρεί ένα POI από τα αγαπημένα του χρήστη.
     */
    suspend fun removeFavoritePoi(poiId: String) {
        val uid = userId()
        if (uid.isBlank()) return
        userPois(uid).document(poiId).delete().await()
    }

    /**
     * Επιστρέφει όλες τις αναφορές στα αγαπημένα POIs του χρήστη.
     */
    suspend fun getFavoritePois(): List<DocumentReference> {
        val uid = userId()
        if (uid.isBlank()) return emptyList()
        val snap = userPois(uid).get().await()
        return snap.documents.mapNotNull { it.getDocumentReference("poiRef") }
    }
}

