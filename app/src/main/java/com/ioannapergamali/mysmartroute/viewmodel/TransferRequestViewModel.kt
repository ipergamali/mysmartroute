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
        passengerId: String,
        driverId: String,
        date: Long,
        cost: Double,
    ) {
        val entity = TransferRequestEntity(
            routeId = routeId,
            passengerId = passengerId,
            driverId = driverId,
            date = date,
            cost = cost,
            status = RequestStatus.PENDING
        )
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).transferRequestDao()
            try {
                Log.d(TAG, "Εισαγωγή αιτήματος: $entity")
                val id = dao.insert(entity)
                Log.d(TAG, "Το αίτημα αποθηκεύτηκε τοπικά με id=$id")
                val saved = entity.copy(requestNumber = id.toInt())
                db.collection("transfer_requests")
                    .document(id.toString())
                    .set(saved.toFirestoreMap())
                    .await()
                Log.d(TAG, "Το αίτημα αποθηκεύτηκε στο Firestore με id=$id")
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία υποβολής αιτήματος", e)
            }
        }
    }

    fun notifyDriver(context: Context, requestNumber: Int, driverId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).transferRequestDao()
            dao.assignDriver(requestNumber, driverId, RequestStatus.PENDING)
            try {
                db.collection("transfer_requests")
                    .document(requestNumber.toString())
                    .update(
                        mapOf(

                            "status" to RequestStatus.PENDING.name
                        )
                    )
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία ενημέρωσης οδηγού", e)
            }
        }
    }

    fun updateStatus(context: Context, requestNumber: Int, status: RequestStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).transferRequestDao()
            dao.updateStatus(requestNumber, status)
            try {
                db.collection("transfer_requests")
                    .document(requestNumber.toString())
                    .update("status", status.name)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία ενημέρωσης κατάστασης", e)
            }
        }
    }
}
