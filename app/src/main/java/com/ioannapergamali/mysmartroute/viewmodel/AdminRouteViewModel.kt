package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.repository.AdminRouteRepository
import com.ioannapergamali.mysmartroute.repository.RouteDuplicateGroups
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminRouteViewModel(private val repo: AdminRouteRepository) : ViewModel() {
    val duplicateRoutes: StateFlow<RouteDuplicateGroups> =
        repo.getDuplicateRoutes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RouteDuplicateGroups(emptyList(), emptyList())
        )

    fun updateRoute(route: RouteEntity) {
        viewModelScope.launch { repo.updateRoute(route) }
    }

    fun mergeRoutes(keepId: String, removeId: String) {
        viewModelScope.launch { repo.mergeRoutes(keepId, removeId) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = MySmartRouteDatabase.getInstance(context)
            val repo = AdminRouteRepository(db)
            @Suppress("UNCHECKED_CAST")
            return AdminRouteViewModel(repo) as T
        }
    }
}
