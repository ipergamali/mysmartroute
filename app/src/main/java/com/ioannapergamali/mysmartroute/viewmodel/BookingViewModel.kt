package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationDetailEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MovingDetailEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * ViewModel που διαχειρίζεται τη διαδικασία κράτησης θέσεων.
 * ViewModel that manages seat booking operations.
 */
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

    /**
     * Ανανεώνει τις διαθέσιμες διαδρομές από το Firestore.
     * Refreshes available routes from Firestore.
     */
    fun refreshRoutes() {
        db.collection("routes").get().addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { it.toObject(RouteEntity::class.java) }
            _availableRoutes.value = list
        }
    }

    /**
     * Κρατά θέση για τον τρέχοντα χρήστη εφόσον δεν υπάρχει ήδη κράτηση.
     * Reserves a seat for the current user if no reservation exists.
     */
    suspend fun reserveSeat(
        context: Context,
        routeId: String,
        date: Long,
        startTime: Long,
        startPoiId: String,
        endPoiId: String,
        declarationId: String = "",
        driverId: String = "",
        vehicleId: String = "",
        cost: Double? = null,
        durationMinutes: Int = 0
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
            ?: return@withContext Result.failure(Exception("Απαιτείται σύνδεση"))

        val dbInstance = MySmartRouteDatabase.getInstance(context)
        val resDao = dbInstance.seatReservationDao()
        val resDetailDao = dbInstance.seatReservationDetailDao()
        val movingDao = dbInstance.movingDao()
        val movingDetailDao = dbInstance.movingDetailDao()

        // Έλεγχος για ήδη υπάρχουσα κράτηση
        // Check for an existing reservation
        val existing = resDao.findUserReservation(userId, routeId, date, startTime)
        if (existing != null) {
            return@withContext Result.failure(Exception("Η θέση έχει ήδη κρατηθεί"))
        }

        val reservation = SeatReservationEntity(
            id = UUID.randomUUID().toString(),
            declarationId = declarationId,
            routeId = routeId,
            userId = userId,
            date = date,
            startTime = startTime
        )
        val resDetail = SeatReservationDetailEntity(
            id = UUID.randomUUID().toString(),
            reservationId = reservation.id,
            startPoiId = startPoiId,
            endPoiId = endPoiId
        )

        return@withContext try {
            resDao.insert(reservation)
            resDetailDao.insert(resDetail)
            val resRef = db.collection("seat_reservations").document(reservation.id)
            resRef.set(reservation.toFirestoreMap()).await()
            resRef.collection("details")
                .document(resDetail.id)
                .set(resDetail.toFirestoreMap())
                .await()

            val moving = MovingEntity(
                id = UUID.randomUUID().toString(),
                routeId = routeId,
                userId = userId,
                date = date,
                cost = cost,
                durationMinutes = durationMinutes,
                driverId = driverId,
                status = "pending"
            )
            movingDao.insert(moving)
            val movingDetail = MovingDetailEntity(
                id = UUID.randomUUID().toString(),
                movingId = moving.id,
                startPoiId = startPoiId,
                endPoiId = endPoiId,
                vehicleId = vehicleId
            )
            movingDetailDao.insert(movingDetail)
            val movingRef = db.collection("movings").document(moving.id)
            movingRef.set(moving.toFirestoreMap()).await()
            movingRef.collection("details")
                .document(movingDetail.id)
                .set(movingDetail.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία κράτησης", e)
            Result.failure(e)
        }
    }
}

