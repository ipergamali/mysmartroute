package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toRouteWithPoints
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationEntity
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

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

    fun refreshRoutes() {
        db.collection("routes").get().addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { it.toObject(RouteEntity::class.java) }
            _availableRoutes.value = list
        }
    }

    suspend fun reserveSeat(
        context: Context,
        routeId: String,
        date: Long,
        startPoiId: String,
        endPoiId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid

            Result.failure(e)
        }
    }
}
