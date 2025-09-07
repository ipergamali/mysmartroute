package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.ioannapergamali.mysmartroute.repository.FavoritesRepository

/**
 * ViewModel για τη διαχείριση αγαπημένων σημείων ενδιαφέροντος (POIs).
 * Τα αγαπημένα αποθηκεύονται στη διαδρομή
 * `users/{uid}/Favorites/data/pois/{poiId}` ως αναφορά
 * στο πραγματικό έγγραφο του POI.
 */
class FavoritePoisViewModel(
    private val repository: FavoritesRepository = FavoritesRepository()
) : ViewModel() {

    /**
     * Προσθέτει ένα POI στα αγαπημένα του χρήστη.
     */
    suspend fun addFavoritePoi(poiId: String) = repository.saveFavorite(poiId)

    /**
     * Αφαιρεί ένα POI από τα αγαπημένα του χρήστη.
     */
    suspend fun removeFavoritePoi(poiId: String) = repository.removeFavorite(poiId)

    /**
     * Επιστρέφει όλες τις αναφορές στα αγαπημένα POIs του χρήστη.
     */
    suspend fun getFavoritePois(): List<DocumentReference> = repository.getFavoriteRefs()
}

