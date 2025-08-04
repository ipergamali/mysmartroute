package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import kotlinx.coroutines.Dispatchers
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toMovingEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.NotificationUtils
import com.ioannapergamali.mysmartroute.R

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VehicleRequestViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _requests = MutableStateFlow<List<MovingEntity>>(emptyList())
    val requests: StateFlow<List<MovingEntity>> = _requests

    companion object {
        private const val TAG = "VehicleRequestVM"
    }

    fun loadRequests(context: Context, allUsers: Boolean = false) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            _requests.value = if (allUsers) {
                dao.getAll().first()
            } else {
                userId?.let { dao.getMovingsForUser(it).first() } ?: emptyList()
            }

            val snapshot = if (NetworkUtils.isInternetAvailable(context)) {
                runCatching {
                    if (allUsers) {
                        db.collection("movings").get().await()
                    } else if (userId != null) {
                        val userRef = db.collection("users").document(userId)
                        db.collection("movings").whereEqualTo("userId", userRef).get().await()
                    } else null
                }.getOrNull()
            } else null

            snapshot?.let { snap ->
                val list = snap.documents.mapNotNull { it.toMovingEntity() }
                if (list.isNotEmpty()) {
                    _requests.value = list
                    list.forEach { dao.insert(it) }
                }
            }
        }
    }

    fun deleteRequests(context: Context, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            dao.deleteByIds(ids.toList())
            _requests.value = _requests.value.filterNot { it.id in ids }
            ids.forEach { id ->
                db.collection("movings").document(id).delete()
            }
        }
    }

    fun requestTransport(
        context: Context,
        routeId: String,
        fromPoiId: String,
        toPoiId: String,
        maxCost: Double,
        date: Long,
        targetUserId: String? = null
    ) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val creator = FirebaseAuth.getInstance().currentUser
            val creatorId = creator?.uid ?: ""
            val creatorName = UserViewModel().getUserName(context, creatorId)
            val userId = targetUserId ?: creatorId
            val id = UUID.randomUUID().toString()
            val entity = MovingEntity(
                id = id,
                routeId = routeId,
                userId = userId,
                date = date,
                vehicleId = "",
                cost = maxCost,
                durationMinutes = 0,
                startPoiId = fromPoiId,
                endPoiId = toPoiId,
                createdById = creatorId,
                createdByName = creatorName
            )
            dao.insert(entity)
            try {
                FirebaseFirestore.getInstance()
                    .collection("movings")
                    .document(id)
                    .set(entity.toFirestoreMap())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to store moving", e)
            }
        }
    }

    fun notifyPassenger(context: Context, requestId: String) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val driver = FirebaseAuth.getInstance().currentUser ?: return@launch
            val driverName = UserViewModel().getUserName(context, driver.uid)
            val list = _requests.value.toMutableList()
            val index = list.indexOfFirst { it.id == requestId }
            if (index != -1) {
                val updated = list[index].copy(driverId = driver.uid, status = "pending").also {
                    it.driverName = driverName
                }
                list[index] = updated
                _requests.value = list
                dao.insert(updated)
                try {
                    db.collection("movings").document(requestId).update(
                        mapOf(
                            "driverId" to FirebaseFirestore.getInstance().collection("users").document(driver.uid),
                            "driverName" to driverName,
                            "status" to "pending"
                        )
                    ).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to notify passenger", e)
                }
            }
        }
    }

    fun respondToOffer(context: Context, requestId: String, accept: Boolean) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val list = _requests.value.toMutableList()
            val index = list.indexOfFirst { it.id == requestId }
            if (index != -1) {
                val current = list[index]

                if (accept) {
                    val booked = BookingViewModel().reserveSeat(
                        context,
                        current.routeId,
                        current.date,
                        current.startPoiId,
                        current.endPoiId
                    )
                    if (!booked) {
                        Log.e(TAG, "Seat reservation failed")
                        return@launch
                    }
                    NotificationUtils.showNotification(
                        context,
                        context.getString(R.string.reserve_seat_title),
                        context.getString(R.string.seat_booked)
                    )
                }

                val status = if (accept) "accepted" else "rejected"
                val updated = current.copy(status = status, driverId = if (accept) current.driverId else "")
                list[index] = updated
                _requests.value = list
                dao.insert(updated)
                try {
                    db.collection("movings").document(requestId).update(
                        mapOf(
                            "status" to status,
                            "driverId" to updated.driverId
                        )
                    ).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to respond to offer", e)
                }
            }
        }
    }

}
