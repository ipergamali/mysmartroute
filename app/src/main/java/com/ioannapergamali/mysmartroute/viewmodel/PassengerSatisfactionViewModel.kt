package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.model.classes.users.PassengerSatisfaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PassengerSatisfactionViewModel : ViewModel() {
    private val _mostSatisfied = MutableStateFlow<List<PassengerSatisfaction>>(emptyList())
    val mostSatisfied: StateFlow<List<PassengerSatisfaction>> = _mostSatisfied

    private val _leastSatisfied = MutableStateFlow<List<PassengerSatisfaction>>(emptyList())
    val leastSatisfied: StateFlow<List<PassengerSatisfaction>> = _leastSatisfied

    fun loadRatings(context: Context) {
        val db = MySmartRouteDatabase.getInstance(context)
        viewModelScope.launch {
            combine(
                db.tripRatingDao().getMostSatisfiedPassengers(),
                db.tripRatingDao().getLeastSatisfiedPassengers()
            ) { top, bottom ->
                val topIds = top.map { it.passengerId }.toSet()
                _mostSatisfied.value = top
                _leastSatisfied.value = bottom.filterNot { it.passengerId in topIds }
            }.collect { }
        }
    }
}
