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
 * Αναπαριστά ένα τμήμα διαδρομής για κράτηση.
 */
data class ReservationSegment(
    val startPoiId: String,
    val endPoiId: String,
    val vehicleId: String,
    val cost: Double,
    val startTime: Long
)

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
        segments: List<ReservationSegment>,
        declarationId: String = "",
        driverId: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
            ?: return@withContext Result.failure(Exception("Απαιτείται σύνδεση"))

        if (segments.isEmpty()) {
            return@withContext Result.failure(Exception("Δεν επιλέχθηκαν στάσεις"))
        }

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

        val totalCost = segments.sumOf { it.cost }
        val durationMinutes = if (segments.size > 1) {
            val startTimes = segments.map { it.startTime }.sorted()
            startTimes.zipWithNext { a, b -> ((b - a) / 60000).toInt() }.sum()
        } else 0

        val reservation = SeatReservationEntity(
            id = UUID.randomUUID().toString(),
            declarationId = declarationId,
            routeId = routeId,
            userId = userId,
            date = date,
            startTime = startTime
        )

        val moving = MovingEntity(
            id = UUID.randomUUID().toString(),
            routeId = routeId,
            userId = userId,
            date = date,
            cost = totalCost,
            durationMinutes = durationMinutes,
            driverId = driverId,
            status = "pending"
        )

        return@withContext try {
            resDao.insert(reservation)
            movingDao.insert(moving)
            val resRef = db.collection("seat_reservations").document(reservation.id)
            resRef.set(reservation.toFirestoreMap()).await()
            val movingRef = db.collection("movings").document(moving.id)
            movingRef.set(moving.toFirestoreMap()).await()

            segments.forEach { seg ->
                val resDetail = SeatReservationDetailEntity(
                    id = UUID.randomUUID().toString(),
                    reservationId = reservation.id,
                    startPoiId = seg.startPoiId,
                    endPoiId = seg.endPoiId,
                    cost = seg.cost,
                    startTime = seg.startTime
                )
                resDetailDao.insert(resDetail)
                resRef.collection("details")
                    .document(resDetail.id)
                    .set(resDetail.toFirestoreMap())
                    .await()

                val movingDetail = MovingDetailEntity(
                    id = UUID.randomUUID().toString(),
                    movingId = moving.id,
                    startPoiId = seg.startPoiId,
                    endPoiId = seg.endPoiId,
                    vehicleId = seg.vehicleId
                )
                movingDetailDao.insert(movingDetail)
                val detailMap = mapOf(
                    "id" to movingDetail.id,
                    "startPoiId" to db.collection("pois").document(seg.startPoiId),
                    "endPoiId" to db.collection("pois").document(seg.endPoiId),
                    "vehicleId" to db.collection("vehicles").document(seg.vehicleId),
                    "cost" to seg.cost,
                    "startTime" to seg.startTime
                )
                movingRef.collection("details")
                    .document(movingDetail.id)
                    .set(detailMap)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία κράτησης", e)
            Result.failure(e)
        }
    }
}

