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
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationEntity
import kotlinx.coroutines.runBlocking
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

    fun reserveSeat(context: Context, routeId: String, date: Long): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val reservationId = UUID.randomUUID().toString()
        return try {
            runBlocking {
                val declarations = db.collection("transport_declarations")
                    .whereEqualTo("routeId", routeId)
                    .whereEqualTo("date", date)
                    .get().await()
                    .documents.mapNotNull { it.toTransportDeclarationEntity() }
                val declaration = declarations.firstOrNull() ?: return@runBlocking false
                val existing = db.collection("seat_reservations")
                    .whereEqualTo("routeId", routeId)
                    .whereEqualTo("date", date)
                    .get().await()
                if (existing.size() >= declaration.seats) {
                    return@runBlocking false
                }
                val reservation = SeatReservationEntity(reservationId, routeId, userId, date)
                db.collection("seat_reservations").document(reservationId)
                    .set(reservation.toFirestoreMap()).await()
                viewModelScope.launch {
                    MySmartRouteDatabase.getInstance(context).seatReservationDao().insert(reservation)
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
