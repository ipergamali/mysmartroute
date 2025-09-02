package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * ViewModel για αποθήκευση και ανάκτηση διαδρομών που ενδιαφέρουν τον επιβάτη.
 * Οι διαδρομές αποθηκεύονται στο υποσυλλογή favorites -> routes του χρήστη.
 */
class FavoriteRoutesViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private fun userRoutes(uid: String) = firestore.collection("users")
        .document(uid)
        .collection("favorites")
        .document("routes")
        .collection("items")

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    /** Φορτώνει τις αγαπημένες διαδρομές του χρήστη. */
    fun loadFavorites() {
        val uid = userId()
        if (uid.isBlank()) return
        viewModelScope.launch {
            val snap = runCatching { userRoutes(uid).get().await() }.getOrNull()
            _favorites.value = snap?.documents
                ?.mapNotNull { it.getString("routeId") }
                ?.toSet() ?: emptySet()
        }
    }

    /** Ενημερώνει τοπικά το σύνολο διαδρομών που ενδιαφέρουν τον χρήστη. */
    fun toggleFavorite(routeId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(routeId)) current.remove(routeId) else current.add(routeId)
        _favorites.value = current
    }

    /** Αποθηκεύει τις επιλεγμένες διαδρομές στη βάση. */
    fun saveFavorites(onComplete: (Boolean) -> Unit = {}) {
        val uid = userId()
        if (uid.isBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val routesRef = userRoutes(uid)
            val result = runCatching {
                val snap = routesRef.get().await()
                snap.documents.forEach { it.reference.delete().await() }
                _favorites.value.forEach { routeId ->
                    val id = UUID.randomUUID().toString()
                    val data = mapOf("id" to id, "routeId" to routeId)
                    routesRef.document(id).set(data).await()
                }
            }.isSuccess
            onComplete(result)
        }
    }
}

