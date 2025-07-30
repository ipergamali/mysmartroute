package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toMovingEntity
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

    fun loadRequests(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            var list = dao.getMovingsForUser(userId).first()

            if (NetworkUtils.isInternetAvailable(context)) {
                val remote = runCatching {
                    db.collection("movings")
                        .whereEqualTo("userId", db.collection("users").document(userId))
                        .get()
                        .await()
                        .documents.mapNotNull { it.toMovingEntity() }
                }.getOrNull()

                if (remote != null) {
                    remote.forEach { dao.insert(it) }
                    list = (list + remote).distinctBy { it.id }
                }
            }

            _requests.value = list
        }
    }
}
