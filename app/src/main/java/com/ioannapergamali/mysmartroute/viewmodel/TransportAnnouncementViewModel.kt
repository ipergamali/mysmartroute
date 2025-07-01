package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.classes.transports.TransportAnnouncement
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransportAnnouncementEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel to handle creation of transport availability announcements.
 */
class TransportAnnouncementViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow<AnnouncementState>(AnnouncementState.Idle)
    val state: StateFlow<AnnouncementState> = _state

    private val _announcements = MutableStateFlow<List<TransportAnnouncement>>(emptyList())
    val announcements: StateFlow<List<TransportAnnouncement>> = _announcements

    fun announce(
        context: Context,
        route: Route,
        vehicleType: VehicleType,
        date: Int,
        cost: Double,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            _state.value = AnnouncementState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _state.value = AnnouncementState.Error("User not logged in")
                return@launch
            }

            if (cost <= 0 || durationMinutes <= 0) {
                _state.value = AnnouncementState.Error("Invalid cost or duration")
                return@launch
            }

            val announcementId = UUID.randomUUID().toString()
            val entity = TransportAnnouncementEntity(
                id = announcementId,
                driverId = userId,
                vehicleType = vehicleType.name,
                start = route.start,
                end = route.end,
                date = date,
                cost = cost,
                durationMinutes = durationMinutes
            )

            val dao = MySmartRouteDatabase.getInstance(context).transportAnnouncementDao()

            if (NetworkUtils.isInternetAvailable(context)) {
                firestore.collection("transport_announcements")
                    .document(announcementId)
                    .set(entity.toFirestoreMap())
                    .addOnSuccessListener {
                        viewModelScope.launch { dao.insert(entity) }
                        _announcements.value = _announcements.value + TransportAnnouncement(
                            announcementId,
                            userId,
                            vehicleType,
                            route,
                            date,
                            cost,
                            durationMinutes
                        )
                        _state.value = AnnouncementState.Success
                    }
                    .addOnFailureListener { e ->
                        _state.value = AnnouncementState.Error(e.localizedMessage ?: "Failed")
                    }
            } else {
                dao.insert(entity)
                _announcements.value = _announcements.value + TransportAnnouncement(
                    announcementId,
                    userId,
                    vehicleType,
                    route,
                    date,
                    cost,
                    durationMinutes
                )
                _state.value = AnnouncementState.Success
            }
        }
    }

    sealed class AnnouncementState {
        object Idle : AnnouncementState()
        object Loading : AnnouncementState()
        object Success : AnnouncementState()
        data class Error(val message: String) : AnnouncementState()
    }
}
