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
 * Οι διαδρομές αποθηκεύονται στο πεδίο "favorites.routes" του εγγράφου χρήστη.
 */
class FavoriteRoutesViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    /** Φορτώνει τις αγαπημένες διαδρομές του χρήστη. */
    fun loadFavorites() {
        val uid = userId()
        if (uid.isBlank()) return
        viewModelScope.launch {
            val snap = runCatching { userDoc(uid).get().await() }.getOrNull()
            val list = snap?.get("favorites.routes") as? List<*>
            _favorites.value = list?.filterIsInstance<String>()?.toSet() ?: emptySet()
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
            val result = runCatching {
                userDoc(uid).update("favorites.routes", _favorites.value.toList()).await()
            }.isSuccess
            onComplete(result)
        }
    }
}

