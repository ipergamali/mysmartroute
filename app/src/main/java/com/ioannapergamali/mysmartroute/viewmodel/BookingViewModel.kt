package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BookingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "BookingVM"
    }

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

    suspend fun reserveSeat(
        context: Context,
        routeId: String,
        date: Long,
        startTime: Long,
        startPoiId: String,
        endPoiId: String,
        declarationId: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
            ?: return@withContext Result.failure(Exception("Απαιτείται σύνδεση"))

        val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()

        // Έλεγχος για ήδη υπάρχουσα κράτηση
        val existing = dao.findUserReservation(userId, routeId, date, startTime)
        if (existing != null) {
            return@withContext Result.failure(Exception("Η θέση έχει ήδη κρατηθεί"))
        }

        val reservation = SeatReservationEntity(
            id = UUID.randomUUID().toString(),
            declarationId = declarationId,
            routeId = routeId,
            userId = userId,
            date = date,
            startTime = startTime,
            startPoiId = startPoiId,
            endPoiId = endPoiId
        )

        return@withContext try {
            dao.insert(reservation)
            db.collection("seat_reservations")
                .document(reservation.id)
                .set(reservation.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία κράτησης", e)
            Result.failure(e)
        }
    }
}

