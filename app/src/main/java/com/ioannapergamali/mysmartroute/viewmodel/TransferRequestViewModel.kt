package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ioannapergamali.mysmartroute.data.local.MovingDetailEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransferRequestDao
import com.ioannapergamali.mysmartroute.data.local.TransferRequestEntity
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import java.util.ArrayDeque
import java.util.UUID

/**
 * ViewModel για υποβολή και διαχείριση αιτημάτων μεταφοράς.
 * ViewModel for submitting and managing transfer requests.
 */
class TransferRequestViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "TransferRequestVM"
    }

    /**
     * Υποβάλλει νέο αίτημα μεταφοράς και το αποθηκεύει σε Room και Firestore.
     * Submits a new transfer request storing it in Room and Firestore.
     */
    fun submitRequest(
        context: Context,
        routeId: String,
        startPoiId: String,
        endPoiId: String,
        date: Long,
        cost: Double?,
        poiChanged: Boolean = false,
    ) {
        val passengerId = SessionManager.currentUserId(auth) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val transferDao = dbInstance.transferRequestDao()
            val movingDao = dbInstance.movingDao()
            val detailDao = dbInstance.movingDetailDao()

            try {
                val localLastNumber = transferDao.getMaxRequestNumber() ?: 0
                val remoteLastNumber = try {
                    db.collection("transfer_requests")
                        .orderBy("requestNumber", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                        ?.getLong("requestNumber")
                        ?.toInt()
                        ?: 0
                } catch (e: Exception) {
                    Log.w(TAG, "Αποτυχία ανάκτησης τελευταίου request number από Firestore", e)
                    0
                }
                val requestNumber = max(localLastNumber, remoteLastNumber) + 1

                val requestEntity = TransferRequestEntity(
                    requestNumber = requestNumber,
                    routeId = routeId,
                    passengerId = passengerId,
                    driverId = "",
                    date = date,
                    cost = cost,
                    status = RequestStatus.OPEN
                )

                transferDao.insert(requestEntity)

                val baseSegments = if (poiChanged) emptyList() else fetchSegments(routeId, startPoiId, endPoiId)
                val duration = baseSegments.sumOf { it.durationMinutes }
                val movingId = UUID.randomUUID().toString()
                val segments = baseSegments.map { it.copy(movingId = movingId) }
                val moving = MovingEntity(
                    id = movingId,
                    routeId = routeId,
                    userId = passengerId,
                    date = date,
                    cost = cost,
                    durationMinutes = duration,
                    startPoiId = startPoiId,
                    endPoiId = endPoiId,
                    driverId = "",
                    status = "open",
                    requestNumber = requestNumber
                )

                movingDao.insert(moving)
                segments.forEach { detailDao.insert(it) }

                val reqRef = db.collection("transfer_requests")
                    .add(requestEntity.toFirestoreMap())
                    .await()
                transferDao.setFirebaseId(requestNumber, reqRef.id)

                val movingRef = db.collection("movings").document(movingId)
                movingRef.set(moving.toFirestoreMap()).await()
                segments.forEach { seg ->
                    movingRef.collection("details")
                        .document(seg.id)
                        .set(seg.toFirestoreMap())
                        .await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία υποβολής αιτήματος", e)
            }
        }
    }

    private suspend fun fetchSegments(
        routeId: String,
        startPoiId: String,
        endPoiId: String,
    ): List<MovingDetailEntity> {
        val routeRef = db.collection("routes").document(routeId)
        val declSnap = db.collection("transport_declarations")
            .whereEqualTo("routeId", routeRef)
            .get()
            .await()
        val rawSegments = mutableListOf<MovingDetailEntity>()
        declSnap.documents.forEach { decl ->
            val dets = decl.reference.collection("details").get().await()
            dets.documents.forEach { doc ->

                val start = when (val rawStart = doc.get("startPoiId")) {
                    is DocumentReference -> rawStart.id
                    is String -> rawStart
                    else -> doc.getString("startPoiId")
                } ?: return@forEach
                val end = when (val rawEnd = doc.get("endPoiId")) {
                    is DocumentReference -> rawEnd.id
                    is String -> rawEnd
                    else -> doc.getString("endPoiId")
                } ?: return@forEach
                val duration = (doc.getLong("durationMinutes") ?: 0L).toInt()
                val vehicle = when (val rawVehicle = doc.get("vehicleId")) {
                    is DocumentReference -> rawVehicle.id
                    is String -> rawVehicle
                    else -> doc.getString("vehicleId")
                } ?: ""

                rawSegments += MovingDetailEntity(
                    id = UUID.randomUUID().toString(),
                    movingId = "",
                    startPoiId = start,
                    endPoiId = end,
                    durationMinutes = duration,
                    vehicleId = vehicle,
                )
            }
        }
        val graph = rawSegments.groupBy { it.startPoiId }
        val queue: ArrayDeque<List<MovingDetailEntity>> = ArrayDeque()
        queue.add(emptyList())
        val visited = mutableSetOf<String>()
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val current = path.lastOrNull()?.endPoiId ?: startPoiId
            if (current == endPoiId) return path
            if (!visited.add(current)) continue
            graph[current].orEmpty().forEach { seg ->
                queue.add(path + seg)
            }
        }
        return emptyList()
    }

    /**
     * Ενημερώνει τον οδηγό ότι έχει αναλάβει αίτημα μεταφοράς.
     * Notifies the driver that they have taken a transfer request.
     */
    fun notifyDriver(context: Context, requestNumber: Int) {
        val driverId = SessionManager.currentUserId(auth) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.transferRequestDao()
            val userDao = dbInstance.userDao()
            val request = runCatching { dao.getRequestByNumber(requestNumber) }.getOrNull()
            val driver = userDao.getUser(driverId)
            val driverName = driver?.let { "${it.name} ${it.surname}" } ?: ""
            if (request != null) {
                dao.assignDriver(requestNumber, driverId, driverName, RequestStatus.PENDING)
            }
            val firebaseId = resolveFirebaseId(dao, request, requestNumber)
            if (!firebaseId.isNullOrBlank()) {
                try {
                    db.collection("transfer_requests")
                        .document(firebaseId)
                        .update(
                            mapOf(
                                "driverId" to driverId,
                                "driverName" to driverName,
                                "status" to RequestStatus.PENDING.name
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Αποτυχία ενημέρωσης οδηγού", e)
                }
            } else {
                Log.w(TAG, "Δεν βρέθηκε Firebase id για αίτημα $requestNumber")
            }
        }
    }

    /**
     * Ενημερώνει την κατάσταση ενός αιτήματος μεταφοράς.
     * Updates the status of a transfer request.
     */
    fun updateStatus(context: Context, requestNumber: Int, status: RequestStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).transferRequestDao()
            val request = runCatching { dao.getRequestByNumber(requestNumber) }.getOrNull()
            if (request != null) {
                dao.updateStatus(requestNumber, status)
            }
            val firebaseId = resolveFirebaseId(dao, request, requestNumber)
            if (!firebaseId.isNullOrBlank()) {
                try {
                    db.collection("transfer_requests")
                        .document(firebaseId)
                        .update("status", status.name)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Αποτυχία ενημέρωσης κατάστασης", e)
                }
            } else {
                Log.w(TAG, "Δεν βρέθηκε Firebase id για αίτημα $requestNumber")
            }
        }
    }

    private suspend fun resolveFirebaseId(
        dao: TransferRequestDao,
        request: TransferRequestEntity?,
        requestNumber: Int
    ): String? {
        request?.firebaseId?.takeIf { it.isNotBlank() }?.let { return it }
        return try {
            val snapshot = db.collection("transfer_requests")
                .whereEqualTo("requestNumber", requestNumber)
                .limit(1)
                .get()
                .await()
            val firebaseId = snapshot.documents.firstOrNull()?.id
            if (firebaseId != null && request != null && request.firebaseId.isBlank()) {
                dao.setFirebaseId(requestNumber, firebaseId)
            }
            firebaseId
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία εντοπισμού Firebase id για αίτημα $requestNumber", e)
            null
        }
    }
}
