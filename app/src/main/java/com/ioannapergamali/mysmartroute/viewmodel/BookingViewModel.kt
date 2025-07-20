package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _availableRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val availableRoutes: StateFlow<List<RouteEntity>> = _availableRoutes

    init {
        db.collection("routes").get().addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { it.toObject(RouteEntity::class.java) }
            _availableRoutes.value = list
        }
    }

    fun reserveSeat(routeId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val routeRef = db.collection("served_movings").document(routeId)
        return try {
            db.runTransaction { tx ->
                val served = tx.get(routeRef).toObject(com.ioannapergamali.mysmartroute.model.classes.transports.ServedMoving::class.java)
                    ?: return@runTransaction false
                if (served.hasAvailableSeat(capacity = 4)) {
                    served.passengers.add(userId)
                    tx.set(routeRef, served)
                    true
                } else false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
