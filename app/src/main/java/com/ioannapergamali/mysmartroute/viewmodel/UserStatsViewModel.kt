package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class UserSummary(
    val user: UserEntity,
    val completedMovings: List<MovingEntity>,
    val totalCost: Double,
    val passengerAverageRating: Double,
    val vehicles: List<VehicleEntity>,
    val driverAverageRating: Double
)

class UserStatsViewModel : ViewModel() {
    private val _userSummaries = MutableStateFlow<List<UserSummary>>(emptyList())
    val userSummaries: StateFlow<List<UserSummary>> = _userSummaries

    fun load(context: Context) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val userFlow = db.userDao().getAllUsers()
            val movingFlow = db.movingDao().getAll()
            val ratingFlow = db.tripRatingDao().getAll()
            val vehicleFlow = db.vehicleDao().getAllVehicles()
            val routeFlow = db.routeDao().getAll()

            combine(userFlow, movingFlow, ratingFlow, vehicleFlow, routeFlow) { users, movings, ratings, vehicles, routes ->
                val ratingMap = ratings.associateBy { it.movingId }
                val routeMap = routes.associateBy { it.id }
                val movingsByUser = movings.groupBy { it.userId }
                val movingsByDriver = movings.groupBy { it.driverId }
                val vehiclesByUser = vehicles.groupBy { it.userId }
                users.map { user ->
                    val userMovings = movingsByUser[user.id]?.filter { it.status == "completed" } ?: emptyList()
                    userMovings.forEach { it.routeName = routeMap[it.routeId]?.name ?: "" }
                    val totalCost = userMovings.sumOf { it.cost }
                    val passengerRatings = userMovings.mapNotNull { ratingMap[it.id]?.rating }
                    val passengerAvg = if (passengerRatings.isNotEmpty()) passengerRatings.average() else 0.0
                    val userVehicles = vehiclesByUser[user.id] ?: emptyList()
                    val driverMovings = movingsByDriver[user.id]?.filter { it.status == "completed" } ?: emptyList()
                    val driverRatings = driverMovings.mapNotNull { ratingMap[it.id]?.rating }
                    val driverAvg = if (driverRatings.isNotEmpty()) driverRatings.average() else 0.0
                    UserSummary(user, userMovings, totalCost, passengerAvg, userVehicles, driverAvg)
                }
            }.collect { _userSummaries.value = it }
        }
    }
}

