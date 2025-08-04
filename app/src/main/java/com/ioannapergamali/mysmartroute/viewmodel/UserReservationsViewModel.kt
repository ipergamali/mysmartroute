package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toSeatReservationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel που φορτώνει τις κρατήσεις του τρέχοντος επιβάτη.
 */
class UserReservationsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** Πληροφορίες που χρειάζονται για την εμφάνιση μιας κράτησης. */
    data class ReservationInfo(
        val passengerName: String,
        val driverName: String,
        val routeName: String,
        val date: Long,
        val cost: Double
    )

    private val _reservations = MutableStateFlow<List<ReservationInfo>>(emptyList())
    val reservations: StateFlow<List<ReservationInfo>> = _reservations

    fun load(context: Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            buildReservationInfo(dbLocal, userId)

            if (NetworkUtils.isInternetAvailable(context)) {
                val userRef = db.collection("users").document(userId)
                val remote = db.collection("seat_reservations")
                    .whereEqualTo("userId", userRef)
                    .get()
                    .await()
                val list = remote.documents.mapNotNull { it.toSeatReservationEntity() }
                if (list.isNotEmpty()) {
                    val dao = dbLocal.seatReservationDao()
                    list.forEach { dao.insert(it) }
                    buildReservationInfo(dbLocal, userId)
                }
            }
        }
    }

    private suspend fun buildReservationInfo(
        dbLocal: MySmartRouteDatabase,
        userId: String
    ) {
        val seatDao = dbLocal.seatReservationDao()
        val declarations = dbLocal.transportDeclarationDao().getAll().first()
        val routes = dbLocal.routeDao().getAll().first()
        val users = dbLocal.userDao().getAllUsers().first()

        val passenger = users.find { it.id == userId }
        _reservations.value = seatDao.getReservationsForUser(userId).first().map { res ->
            val declaration = declarations.find { it.id == res.declarationId }
            val driver = users.find { it.id == declaration?.driverId }
            val route = routes.find { it.id == res.routeId }
            ReservationInfo(
                passengerName = listOfNotNull(passenger?.name, passenger?.surname).joinToString(" "),
                driverName = listOfNotNull(driver?.name, driver?.surname).joinToString(" "),
                routeName = route?.name ?: res.routeId,
                date = res.date,
                cost = declaration?.cost ?: 0.0
            )
        }
    }
}

