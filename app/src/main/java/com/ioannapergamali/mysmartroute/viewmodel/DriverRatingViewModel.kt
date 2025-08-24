package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.model.classes.users.DriverRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DriverRatingViewModel : ViewModel() {
    private val _bestDrivers = MutableStateFlow<List<DriverRating>>(emptyList())
    val bestDrivers: StateFlow<List<DriverRating>> = _bestDrivers

    private val _worstDrivers = MutableStateFlow<List<DriverRating>>(emptyList())
    val worstDrivers: StateFlow<List<DriverRating>> = _worstDrivers

    fun loadRatings(context: Context) {
        val db = MySmartRouteDatabase.getInstance(context)
        viewModelScope.launch {
            combine(
                db.tripRatingDao().getTopDrivers(),
                db.tripRatingDao().getWorstDrivers()
            ) { top, worst ->
                val topIds = top.map { it.driverId }.toSet()
                _bestDrivers.value = top
                _worstDrivers.value = worst.filterNot { it.driverId in topIds }
            }.collect()
        }
    }
}
