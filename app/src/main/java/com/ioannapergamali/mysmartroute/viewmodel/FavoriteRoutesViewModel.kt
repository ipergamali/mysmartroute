package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.FavoriteRouteEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    private fun userId() = SessionManager.currentUserId() ?: ""

    private fun routesCollection(uid: String) =
        userDoc(uid).collection("favorites").document("data").collection("routes")

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    /**
     * Φορτώνει τις αγαπημένες διαδρομές του χρήστη.
     * Loads the user's favorite routes.
     */
    fun loadFavorites(context: Context) {
        val uid = userId()
        if (uid.isBlank()) return
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).favoriteRouteDao()
            val snap = runCatching { routesCollection(uid).get().await() }.getOrNull()
            if (snap != null) {
                val ids = snap.documents.map { it.id }.toSet()
                _favorites.value = ids
                dao.deleteAllForUser(uid)
                ids.forEach { dao.insert(FavoriteRouteEntity(uid, it)) }
            } else {
                _favorites.value = dao.getFavoritesForUser(uid).first().toSet()
            }
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
    fun saveFavorites(context: Context, onComplete: (Boolean) -> Unit = {}) {
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

            val dao = MySmartRouteDatabase.getInstance(context).favoriteRouteDao()
            dao.deleteAllForUser(uid)
            routesList.forEach { dao.insert(FavoriteRouteEntity(uid, it)) }

            onComplete(result)
        }
    }
}

