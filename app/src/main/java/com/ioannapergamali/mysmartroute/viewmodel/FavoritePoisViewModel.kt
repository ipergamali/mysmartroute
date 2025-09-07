package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import com.ioannapergamali.mysmartroute.repository.FavoritesRepository

/**
 * ViewModel για τη διαχείριση αγαπημένων σημείων ενδιαφέροντος (POIs).
 * Τα αγαπημένα αποθηκεύονται τοπικά στη βάση Room μέσα από το `FavoritesRepository`.
 */
class FavoritePoisViewModel(
    private val repository: FavoritesRepository
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
    fun getFavoritePoiIds(): Flow<List<String>> = repository.getFavoriteIds()
}

