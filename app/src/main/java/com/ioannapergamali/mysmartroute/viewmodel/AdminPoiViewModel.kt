package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.repository.AdminPoiRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel για τον έλεγχο και τη διαχείριση των ονομάτων των σημείων ενδιαφέροντος.
 */
class AdminPoiViewModel(private val repo: AdminPoiRepository) : ViewModel() {
    val pois: StateFlow<List<PoIEntity>> = repo.getAllPois().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /** Λίστα με τα ονόματα των σημείων για γρήγορη επισκόπηση. */
    val poiNames: StateFlow<List<String>> = repo.getPoiNames().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun updatePoi(poi: PoIEntity) {
        viewModelScope.launch { repo.updatePoi(poi) }
    }

    fun mergePois(keepId: String, removeId: String) {
        viewModelScope.launch { repo.mergePois(keepId, removeId) }
    }

    fun deletePoi(id: String) {
        viewModelScope.launch { repo.deletePoi(id) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = MySmartRouteDatabase.getInstance(context)
            val repo = AdminPoiRepository(db)
            @Suppress("UNCHECKED_CAST")
            return AdminPoiViewModel(repo) as T
        }
    }
}
