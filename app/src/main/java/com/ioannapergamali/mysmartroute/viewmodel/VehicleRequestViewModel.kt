package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toMovingEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils

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

    fun requestTransport(context: Context, fromPoiId: String, toPoiId: String, maxCost: Double) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val id = UUID.randomUUID().toString()
            val entity = MovingEntity(
                id = id,
                routeId = "${'$'}fromPoiId-${'$'}toPoiId",
                userId = userId,
                date = 0,
                vehicleId = "",
                cost = maxCost,
                durationMinutes = 0
            )
            dao.insert(entity)
            try {
                FirebaseFirestore.getInstance()
                    .collection("movings")
                    .document(id)
                    .set(entity.toFirestoreMap())
                    .await()
            } catch (_: Exception) {
            }
        }
    }

}
