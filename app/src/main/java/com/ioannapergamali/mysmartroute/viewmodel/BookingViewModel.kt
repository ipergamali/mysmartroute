package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BookingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _availableRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val availableRoutes: StateFlow<List<RouteEntity>> = _availableRoutes

    init {
        refreshRoutes()
    }

    fun refreshRoutes() {
        db.collection("routes").get().addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { it.toObject(RouteEntity::class.java) }
            _availableRoutes.value = list
        }
    }

    fun reserveSeat(context: Context, routeId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val routeRef = db.collection("served_movings").document(routeId)
        val reservationId = UUID.randomUUID().toString()
        return try {
            db.runTransaction { tx ->
                val served = tx.get(routeRef)
                    .toObject(com.ioannapergamali.mysmartroute.model.classes.transports.ServedMoving::class.java)
                    ?: return@runTransaction false
                if (served.hasAvailableSeat(capacity = 4)) {
                    served.passengers.add(userId)
                    tx.set(routeRef, served)
                    true
                } else false
            }

            val reservation = SeatReservationEntity(reservationId, routeId, userId)
            viewModelScope.launch {
                MySmartRouteDatabase.getInstance(context).seatReservationDao().insert(reservation)
            }
            db.collection("seat_reservations").document(reservationId)
                .set(reservation.toFirestoreMap())
            true
        } catch (e: Exception) {
            false
        }
    }
}
