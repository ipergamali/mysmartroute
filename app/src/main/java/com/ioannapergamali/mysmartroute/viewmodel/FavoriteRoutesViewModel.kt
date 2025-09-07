package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel για αποθήκευση και ανάκτηση διαδρομών που ενδιαφέρουν τον επιβάτη.
 * ViewModel for storing and retrieving routes that interest the passenger.
 *
 * Οι διαδρομές αποθηκεύονται στη διαδρομή
 * `users/{uid}/favorites/data/routes/{routeId}` ως αναφορές σε έγγραφα της
 * συλλογής `routes`.
 * Routes are stored at `users/{uid}/favorites/data/routes/{routeId}` as references
 * to documents in the `routes` collection.
 */
class FavoriteRoutesViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private fun routesCollection(uid: String) =
        userDoc(uid).collection("favorites").document("data").collection("routes")

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    /**
     * Φορτώνει τις αγαπημένες διαδρομές του χρήστη.
     * Loads the user's favorite routes.
     */
    fun loadFavorites() {
        val uid = userId()
        if (uid.isBlank()) return
        viewModelScope.launch {
            val snap = runCatching { routesCollection(uid).get().await() }.getOrNull()
            _favorites.value = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
        }
    }

    /**
     * Ενημερώνει τοπικά το σύνολο διαδρομών που ενδιαφέρουν τον χρήστη.
     * Locally updates the set of routes the user is interested in.
     */
    fun toggleFavorite(routeId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(routeId)) current.remove(routeId) else current.add(routeId)
        _favorites.value = current
    }

    /**
     * Αποθηκεύει τις επιλεγμένες διαδρομές στη βάση.
     * Persists the selected routes to the database.
     */
    fun saveFavorites(onComplete: (Boolean) -> Unit = {}) {
        val uid = userId()
        if (uid.isBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val routesList = _favorites.value.toList()
            val result = runCatching {
                val col = routesCollection(uid)
                val batch = firestore.batch()
                val existing = col.get().await()
                existing.documents.forEach { batch.delete(it.reference) }
                routesList.forEach { routeId ->
                    val ref = firestore.collection("routes").document(routeId)
                    batch.set(col.document(routeId), mapOf("routeRef" to ref))
                }
                batch.commit().await()
            }.isSuccess
            onComplete(result)
        }
    }
}

