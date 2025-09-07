package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel για τη διαχείριση αγαπημένων σημείων ενδιαφέροντος.
 * Τα αγαπημένα αποθηκεύονται τοπικά στη βάση Room.
 */
class FavoritePoisViewModel : ViewModel() {
    private var repository: FavoritesRepository? = null
    private fun repo(context: Context): FavoritesRepository {
        return repository ?: FavoritesRepository(MySmartRouteDatabase.getInstance(context)).also {
            repository = it
        }
    }

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    /** Φόρτωση αγαπημένων σημείων του χρήστη. */
    fun loadFavorites(context: Context) {
        viewModelScope.launch {
            repo(context).getFavoriteIds().collect { ids ->
                _favorites.value = ids.toSet()
            }
        }
    }

    /** Τοπική εναλλαγή αγαπημένου σημείου. */
    fun toggleFavorite(poiId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(poiId)) current.remove(poiId) else current.add(poiId)
        _favorites.value = current
    }

    /** Αποθήκευση των επιλεγμένων αγαπημένων. */
    fun saveFavorites(context: Context, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = runCatching {
                repo(context).replaceFavorites(_favorites.value.toList())
            }.isSuccess
            onComplete(result)
        }
    }
}
