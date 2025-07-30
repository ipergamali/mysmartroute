package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VehicleRequestViewModel : ViewModel() {

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
