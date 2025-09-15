package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransferRequestEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MovingDetailEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationDetailEntity
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
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
        cost: Double?,
        date: Long,
    ) {
        val passengerId = auth.currentUser?.uid ?: return
        val entity = TransferRequestEntity(
            routeId = routeId,
            passengerId = passengerId,
            driverId = "",
            date = date,
            cost = cost,
            status = RequestStatus.OPEN
        )
        viewModelScope.launch(Dispatchers.IO) {
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val transferDao = dbLocal.transferRequestDao()
            val movingDao = dbLocal.movingDao()
            val movingDetailDao = dbLocal.movingDetailDao()
            val declDao = dbLocal.transportDeclarationDao()
            val detailDao = dbLocal.transportDeclarationDetailDao()
            try {
                Log.d(TAG, "Εισαγωγή αιτήματος: $entity")
                val requestId = transferDao.insert(entity).toInt()
                Log.d(TAG, "Το αίτημα αποθηκεύτηκε τοπικά με id=$requestId")
                val saved = entity.copy(requestNumber = requestId)
                val docRef = db.collection("transfer_requests")
                    .add(saved.toFirestoreMap())
                    .await()
                transferDao.setFirebaseId(requestId, docRef.id)

                // Αναζήτηση διαδρομής στις δηλώσεις μεταφοράς
                val declaration = declDao.getAll().first().firstOrNull { it.routeId == routeId }
                val declDetails = declaration?.let { detailDao.getForDeclaration(it.id) } ?: emptyList()
                val path = buildPath(declDetails, startPoiId, endPoiId)
                val totalDuration = path.sumOf { it.durationMinutes }
                val totalCost = cost ?: path.sumOf { it.cost }

                val movingId = UUID.randomUUID().toString()
                val moving = MovingEntity(
                    id = movingId,
                    routeId = routeId,
                    userId = passengerId,
                    date = date,
                    cost = totalCost,
                    durationMinutes = totalDuration,
                    driverId = "",
                    status = "open",
                    requestNumber = requestId,
                    vehicleId = path.firstOrNull()?.vehicleId ?: "",
                    startPoiId = startPoiId,
                    endPoiId = endPoiId,
                )
                movingDao.insert(moving)
                val movingRef = db.collection("movings").document(movingId)
                movingRef.set(moving.toFirestoreMap()).await()
                path.forEach { det ->
                    val movingDetail = MovingDetailEntity(
                        id = UUID.randomUUID().toString(),
                        movingId = movingId,
                        startPoiId = det.startPoiId,
                        endPoiId = det.endPoiId,
                        durationMinutes = det.durationMinutes,
                        vehicleId = det.vehicleId,
                    )
                    movingDetailDao.insert(movingDetail)
                    movingRef.collection("details")
                        .document(movingDetail.id)
                        .set(movingDetail.toFirestoreMap())
                        .await()
                }

                Log.d(TAG, "Το αίτημα αποθηκεύτηκε στο Firestore με id=${docRef.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία υποβολής αιτήματος", e)
            }
        }
    }

    private fun buildPath(
        details: List<TransportDeclarationDetailEntity>,
        start: String,
        end: String,
    ): List<TransportDeclarationDetailEntity> {
        val map = details.associateBy { it.startPoiId }
        val path = mutableListOf<TransportDeclarationDetailEntity>()
        var current = start
        val visited = mutableSetOf<String>()
        while (current != end && visited.add(current)) {
            val next = map[current] ?: break
            path += next
            current = next.endPoiId
        }
        return if (path.lastOrNull()?.endPoiId == end) path else emptyList()
    }

    /**
     * Ενημερώνει τον οδηγό ότι έχει αναλάβει αίτημα μεταφοράς.
     * Notifies the driver that they have taken a transfer request.
     */
    fun notifyDriver(context: Context, requestNumber: Int) {
        val driverId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.transferRequestDao()
            val userDao = dbInstance.userDao()
            val request = dao.getRequestByNumber(requestNumber) ?: return@launch
            val driver = userDao.getUser(driverId)
            val driverName = driver?.let { "${it.name} ${it.surname}" } ?: ""
            dao.assignDriver(requestNumber, driverId, driverName, RequestStatus.PENDING)
            try {
                if (request.firebaseId.isNotBlank()) {
                    db.collection("transfer_requests")
                        .document(request.firebaseId)
                        .update(
                            mapOf(
                                "driverId" to driverId,
                                "driverName" to driverName,
                                "status" to RequestStatus.PENDING.name
                            )
                        )
                        .await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία ενημέρωσης οδηγού", e)
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
            val request = dao.getRequestByNumber(requestNumber) ?: return@launch
            dao.updateStatus(requestNumber, status)
            try {
                if (request.firebaseId.isNotBlank()) {
                    db.collection("transfer_requests")
                        .document(request.firebaseId)
                        .update("status", status.name)
                        .await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία ενημέρωσης κατάστασης", e)
            }
        }
    }
}
