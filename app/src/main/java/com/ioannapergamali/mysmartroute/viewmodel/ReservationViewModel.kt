package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toSeatReservationEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ReservationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _reservations = MutableStateFlow<List<SeatReservationEntity>>(emptyList())
    val reservations: StateFlow<List<SeatReservationEntity>> = _reservations

    fun loadReservations(context: Context, routeId: String, date: Long, startTime: Long, declarationId: String) {
        viewModelScope.launch {
            if (declarationId.isBlank()) {
                _reservations.value = emptyList()
                return@launch
            }

            val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()
            _reservations.value = dao.getReservationsForDeclaration(declarationId).first()

            if (NetworkUtils.isInternetAvailable(context)) {
                val routeRef = db.collection("routes").document(routeId)
                val declRef = db.collection("transport_declarations").document(declarationId)
                val remote = db.collection("seat_reservations")
                    .whereEqualTo("routeId", routeRef)
                    .whereEqualTo("date", date)
                    .whereEqualTo("startTime", startTime)
                    .whereEqualTo("declarationId", declRef)
                    .get()
                    .await()
                val list = remote.documents.mapNotNull { it.toSeatReservationEntity() }
                if (list.isNotEmpty()) {
                    _reservations.value = list
                    list.forEach { dao.insert(it) }
                }
            }
        }
    }

    /** Επιστρέφει τον αριθμό κρατήσεων για μια δήλωση μεταφοράς. */
    suspend fun getReservationCount(context: Context, declarationId: String): Int {
        if (declarationId.isBlank()) return 0

        val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()

        val localCount = withContext(Dispatchers.IO) {
            dao.getReservationsForDeclaration(declarationId).first().size
        }

        if (NetworkUtils.isInternetAvailable(context)) {
            val declRef = db.collection("transport_declarations").document(declarationId)
            val remote = db.collection("seat_reservations")
                .whereEqualTo("declarationId", declRef)
                .get()
                .await()
            val list = remote.documents.mapNotNull { it.toSeatReservationEntity() }
            if (list.isNotEmpty()) {
                withContext(Dispatchers.IO) { list.forEach { dao.insert(it) } }
                return list.size
            }
        }

        return localCount
    }

    fun completeRoute(
        context: Context,
        routeId: String,
        date: Long,
        startTime: Long,
        declaration: TransportDeclarationEntity
    ) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val resDao = db.seatReservationDao()
            val movingDao = db.movingDao()
            val reservations = resDao.getReservationsForRouteAndDateTime(routeId, date, startTime).first()
            reservations.forEach { res ->
                val moving = MovingEntity(
                    id = UUID.randomUUID().toString(),
                    routeId = routeId,
                    userId = res.userId,
                    date = date,
                    vehicleId = declaration.vehicleId,
                    cost = declaration.cost,
                    durationMinutes = declaration.durationMinutes,
                    startPoiId = res.startPoiId,
                    endPoiId = res.endPoiId,
                    driverId = declaration.driverId,
                    status = "completed"
                )
                movingDao.insert(moving)
                if (NetworkUtils.isInternetAvailable(context)) {
                    FirebaseFirestore.getInstance()
                        .collection("movings")
                        .document(moving.id)
                        .set(moving.toFirestoreMap())
                        .await()
                }
            }
        }
    }
}
