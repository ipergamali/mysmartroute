package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationDetailEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MovingDetailEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toSeatReservationEntity
import com.ioannapergamali.mysmartroute.utils.toSeatReservationDetailEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel για διαχείριση κρατήσεων θέσεων και ολοκλήρωση διαδρομών.
 * ViewModel managing seat reservations and route completion.
 */
class ReservationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _reservations = MutableStateFlow<List<SeatReservationEntity>>(emptyList())
    val reservations: StateFlow<List<SeatReservationEntity>> = _reservations

    /**
     * Φορτώνει τις κρατήσεις μιας διαδρομής είτε τοπικά είτε από το cloud.
     * Loads reservations for a route locally or from the cloud.
     */
    fun loadReservations(context: Context, routeId: String, date: Long, startTime: Long, declarationId: String) {
        viewModelScope.launch {
            if (declarationId.isBlank()) {
                _reservations.value = emptyList()
                return@launch
            }

            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val dao = dbLocal.seatReservationDao()
            val detailDao = dbLocal.seatReservationDetailDao()
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
                    remote.documents.forEach { doc ->
                        val resId = doc.getString("id") ?: doc.id
                        val dets = doc.reference.collection("details").get().await()
                        dets.documents.mapNotNull { it.toSeatReservationDetailEntity(resId) }
                            .forEach { detailDao.insert(it) }
                    }
                }
            }
        }
    }

    /**
     * Επιστρέφει τον αριθμό κρατήσεων για μια δήλωση μεταφοράς.
     * Returns the reservation count for a transport declaration.
     */
    suspend fun getReservationCount(context: Context, declarationId: String): Int {
        if (declarationId.isBlank()) return 0

        val dbLocal = MySmartRouteDatabase.getInstance(context)
        val dao = dbLocal.seatReservationDao()
        val detailDao = dbLocal.seatReservationDetailDao()

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
                withContext(Dispatchers.IO) {
                    list.forEach { dao.insert(it) }
                    remote.documents.forEach { doc ->
                        val resId = doc.getString("id") ?: doc.id
                        val dets = doc.reference.collection("details").get().await()
                        dets.documents.mapNotNull { it.toSeatReservationDetailEntity(resId) }
                            .forEach { detailDao.insert(it) }
                    }
                }
                return list.size
            }
        }

        return localCount
    }

    /**
     * Επιστρέφει τον αριθμό κρατήσεων για συγκεκριμένο τμήμα διαδρομής.
     * Returns reservation count for a specific segment.
     */
    suspend fun getReservationCountForSegment(
        context: Context,
        declarationId: String,
        startPoiId: String,
        endPoiId: String
    ): Int {
        if (declarationId.isBlank()) return 0

        val dbLocal = MySmartRouteDatabase.getInstance(context)
        val resDao = dbLocal.seatReservationDao()
        val detailDao = dbLocal.seatReservationDetailDao()

        val localCount = withContext(Dispatchers.IO) {
            resDao.getReservationsForDeclaration(declarationId).first().count { res ->
                detailDao.getForReservation(res.id).first().any {
                    it.startPoiId == startPoiId && it.endPoiId == endPoiId
                }
            }
        }

        if (NetworkUtils.isInternetAvailable(context)) {
            val declRef = db.collection("transport_declarations").document(declarationId)
            val remote = db.collection("seat_reservations")
                .whereEqualTo("declarationId", declRef)
                .get()
                .await()
            var remoteCount = 0
            remote.documents.forEach { doc ->
                val dets = doc.reference.collection("details")
                    .whereEqualTo("startPoiId", startPoiId)
                    .whereEqualTo("endPoiId", endPoiId)
                    .get()
                    .await()
                if (!dets.isEmpty) remoteCount++
            }
            return maxOf(localCount, remoteCount)
        }

        return localCount
    }

    /**
     * Ολοκληρώνει μια διαδρομή καταχωρώντας μετακινήσεις και ενημερώνοντας αιτήματα.
     * Completes a route by recording movings and updating requests.
     */
    fun completeRoute(
        context: Context,
        routeId: String,
        date: Long,
        startTime: Long,
        declaration: TransportDeclarationEntity,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val resDao = db.seatReservationDao()
            val resDetailDao = db.seatReservationDetailDao()
            val movingDao = db.movingDao()
            val movingDetailDao = db.movingDetailDao()
            val transferDao = db.transferRequestDao()
            val hasInternet = NetworkUtils.isInternetAvailable(context)
            val existing = movingDao.countCompletedForRoute(routeId, date)
            if (existing > 0) {
                withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }
            val reservations = resDao.getReservationsForRouteAndDateTime(routeId, date, startTime).first()
            val currentMovings = movingDao.getAll().first()
            reservations.forEach { res ->
                val detail = resDetailDao
                    .getForReservation(res.id)
                    .firstOrNull()
                    ?.firstOrNull() ?: return@forEach
                val found = currentMovings.find {
                    it.routeId == routeId &&
                        it.userId == res.userId &&
                        it.date == date
                }
                val moving = if (found != null) {
                    found.copy(
                        cost = declaration.cost,
                        durationMinutes = declaration.durationMinutes,
                        driverId = declaration.driverId,
                        status = "completed"
                    )
                } else {
                    MovingEntity(
                        id = UUID.randomUUID().toString(),
                        routeId = routeId,
                        userId = res.userId,
                        date = date,
                        cost = declaration.cost,
                        durationMinutes = declaration.durationMinutes,
                        driverId = declaration.driverId,
                        status = "completed"
                    )
                }
                movingDao.insert(moving)
                val movingDetail = MovingDetailEntity(
                    id = UUID.randomUUID().toString(),
                    movingId = moving.id,
                    startPoiId = detail.startPoiId,
                    endPoiId = detail.endPoiId,
                    durationMinutes = declaration.durationMinutes,
                    vehicleId = declaration.vehicleId
                )
                movingDetailDao.insert(movingDetail)
                if (moving.requestNumber != 0) {
                    transferDao.updateStatus(moving.requestNumber, RequestStatus.COMPLETED)
                }
                if (hasInternet) {
                    val movingRef = FirebaseFirestore.getInstance()
                        .collection("movings")
                        .document(moving.id)
                    movingRef.set(moving.toFirestoreMap()).await()
                    movingRef.collection("details")
                        .document(movingDetail.id)
                        .set(movingDetail.toFirestoreMap())
                        .await()
                    if (moving.requestNumber != 0) {
                        transferDao.getRequestByNumber(moving.requestNumber)
                            ?.firebaseId
                            ?.takeIf { it.isNotBlank() }
                            ?.let { id ->
                                FirebaseFirestore.getInstance()
                                    .collection("transfer_requests")
                                    .document(id)
                                    .update("status", RequestStatus.COMPLETED.name)
                                    .await()
                            }
                    }
                }
            }
            withContext(Dispatchers.Main) { onResult(true) }
        }
    }
}
