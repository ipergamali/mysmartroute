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

    /** Προσθέτει ή αφαιρεί μια διαδρομή από τα αγαπημένα. */
    fun toggleFavorite(routeId: String) {
        val uid = userId()
        if (uid.isBlank()) return
        viewModelScope.launch {
            val current = _favorites.value
            if (current.contains(routeId)) {
                runCatching {
                    userRoutes(uid)
                        .whereEqualTo("routeId", routeId)
                        .get()
                        .await()
                        .documents
                        .forEach { it.reference.delete().await() }
                }
            } else {
                val id = UUID.randomUUID().toString()
                val data = mapOf("id" to id, "routeId" to routeId)
                runCatching { userRoutes(uid).document(id).set(data).await() }
            }
            loadFavorites()
        }
    }
}

