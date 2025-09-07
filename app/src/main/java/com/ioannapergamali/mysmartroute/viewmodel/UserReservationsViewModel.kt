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
 * ViewModel that loads reservations of the current passenger.
 */
class UserReservationsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _reservations = MutableStateFlow<List<SeatReservationEntity>>(emptyList())
    val reservations: StateFlow<List<SeatReservationEntity>> = _reservations

    /**
     * Φορτώνει τις κρατήσεις του τρέχοντος χρήστη από τη βάση και το cloud.
     * Loads current user's reservations from the database and cloud.
     */
    fun load(context: Context) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()
            _reservations.value = dao.getReservationsForUser(userId).first()

            if (NetworkUtils.isInternetAvailable(context)) {
                val userRef = db.collection("users").document(userId)
                val remote = db.collection("seat_reservations")
                    .whereEqualTo("userId", userRef)
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

    /**
     * Διαγράφει μια κράτηση τόσο τοπικά όσο και από το Firestore.
     * Deletes a reservation locally and from Firestore.
     */
    fun delete(context: Context, reservation: SeatReservationEntity) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()
            dao.deleteById(reservation.id)
            db.collection("seat_reservations").document(reservation.id).delete().await()
            _reservations.value = _reservations.value.filterNot { it.id == reservation.id }
        }
    }
}

