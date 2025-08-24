package com.ioannapergamali.mysmartroute.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UserSummary(
    val user: UserEntity,
    val completedMovings: List<MovingEntity>,
    val totalCost: Double,
    val passengerAverageRating: Double,
    val vehicles: List<VehicleEntity>,
    val driverAverageRating: Double
)

class UserStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val _userSummaries = MutableStateFlow<List<UserSummary>>(emptyList())
    val userSummaries: StateFlow<List<UserSummary>> = _userSummaries

    init {

 
        viewModelScope.launch {

            val db = MySmartRouteDatabase.getInstance(application)

            val db = MySmartRouteDatabase.getInstance(context)


            val users = db.userDao().getAllUsers().first()
            val movings = db.movingDao().getAll().first()
            val ratings = db.tripRatingDao().getAll().first()
            val vehicles = db.vehicleDao().getAllVehicles().first()
            val routes = db.routeDao().getAll().first()

            val ratingMap = ratings.associateBy { it.movingId }
            val routeMap = routes.associateBy { it.id }
            val movingsByUser = movings.groupBy { it.userId }
            val movingsByDriver = movings.groupBy { it.driverId }
            val vehiclesByUser = vehicles.groupBy { it.userId }

            _userSummaries.value = users.map { user ->
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
        }
    }
}

