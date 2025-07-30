package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.FavoritesPreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {
    fun preferredFlow(context: Context): Flow<Set<VehicleType>> =
        FavoritesPreferenceManager.preferredFlow(context)

    fun nonPreferredFlow(context: Context): Flow<Set<VehicleType>> =
        FavoritesPreferenceManager.nonPreferredFlow(context)

    fun addPreferred(context: Context, type: VehicleType) {
        viewModelScope.launch { FavoritesPreferenceManager.addPreferred(context, type) }
    }

    fun addNonPreferred(context: Context, type: VehicleType) {
        viewModelScope.launch { FavoritesPreferenceManager.addNonPreferred(context, type) }
    }

    fun removePreferred(context: Context, type: VehicleType) {
        viewModelScope.launch { FavoritesPreferenceManager.removePreferred(context, type) }
    }

    fun removeNonPreferred(context: Context, type: VehicleType) {
        viewModelScope.launch { FavoritesPreferenceManager.removeNonPreferred(context, type) }
    }
}
