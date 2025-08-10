package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransferRequestEntity
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TransferRequestViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "TransferRequestVM"
    }

    fun submitRequest(
        context: Context,
        routeId: String,
        date: Long,
        cost: Double,
    ) {
        val passengerId = auth.currentUser?.uid ?: return
        val entity = TransferRequestEntity(
            routeId = routeId,
            passengerId = passengerId,
            driverId = "",
            date = date,
            cost = cost,
            status = RequestStatus.PENDING
        )
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).transferRequestDao()
            try {
                Log.d(TAG, "Εισαγωγή αιτήματος: $entity")
                val id = dao.insert(entity).toInt()
                Log.d(TAG, "Το αίτημα αποθηκεύτηκε τοπικά με id=$id")
                val saved = entity.copy(requestNumber = id)
                val docRef = db.collection("transfer_requests")
                    .add(saved.toFirestoreMap())
                    .await()
                dao.setFirebaseId(id, docRef.id)
                Log.d(TAG, "Το αίτημα αποθηκεύτηκε στο Firestore με id=${docRef.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία υποβολής αιτήματος", e)
            }
        }
    }

    fun notifyDriver(context: Context, requestNumber: Int) {
        val driverId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).transferRequestDao()
            val request = dao.getRequestByNumber(requestNumber) ?: return@launch
            dao.assignDriver(requestNumber, driverId, RequestStatus.PENDING)
            try {
                if (request.firebaseId.isNotBlank()) {
                    db.collection("transfer_requests")
                        .document(request.firebaseId)
                        .update(
                            mapOf(
                                "driverId" to driverId,
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
