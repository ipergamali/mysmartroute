package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.model.classes.users.DriverRating
import com.ioannapergamali.mysmartroute.utils.TripRatingSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel που παρέχει κορυφαίους και χειρότερους οδηγούς βάσει αξιολογήσεων.
 * ViewModel providing best and worst drivers based on ratings.
 */
class DriverRatingViewModel : ViewModel() {
    private val _bestDrivers = MutableStateFlow<List<DriverRating>>(emptyList())
    val bestDrivers: StateFlow<List<DriverRating>> = _bestDrivers

    private val _worstDrivers = MutableStateFlow<List<DriverRating>>(emptyList())
    val worstDrivers: StateFlow<List<DriverRating>> = _worstDrivers

    /**
     * Φορτώνει τις αξιολογήσεις οδηγών από την τοπική βάση δεδομένων.
     * Loads driver ratings from the local database.
     */
    fun loadRatings(context: Context) {
        val db = MySmartRouteDatabase.getInstance(context)
        viewModelScope.launch {
            val dao = db.tripRatingDao()
            val hasLocalRatings = withContext(Dispatchers.IO) {
                dao.getAll().first().isNotEmpty()
            }
            if (!hasLocalRatings) {
                TripRatingSyncManager.sync(context)
            }

            combine(
                dao.getTopDrivers(),
                dao.getWorstDrivers()
            ) { top, worst ->
                val topIds = top.map { it.driverId }.toSet()
                _bestDrivers.value = top
                _worstDrivers.value = worst.filterNot { it.driverId in topIds }
            }.collect { }
        }
    }
}
