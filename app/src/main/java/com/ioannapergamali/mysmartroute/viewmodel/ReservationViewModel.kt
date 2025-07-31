package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReservationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _reservations = MutableStateFlow<List<SeatReservationEntity>>(emptyList())
    val reservations: StateFlow<List<SeatReservationEntity>> = _reservations

    fun loadReservations(context: Context, routeId: String, date: Long, declarationId: String) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()
            _reservations.value = dao.getReservationsForDeclaration(declarationId).first()

            if (NetworkUtils.isInternetAvailable(context)) {
                val routeRef = db.collection("routes").document(routeId)
                val declRef = db.collection("transport_declarations").document(declarationId)
                val remote = db.collection("seat_reservations")
                    .whereEqualTo("routeId", routeRef)
                    .whereEqualTo("date", date)
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
        val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()
        return withContext(Dispatchers.IO) {
            dao.getReservationsForDeclaration(declarationId).first().size
        }
    }
}
