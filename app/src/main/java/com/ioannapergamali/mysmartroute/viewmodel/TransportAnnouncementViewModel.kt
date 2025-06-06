package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.model.classes.routes.Route
import com.ioannapergamali.mysmartroute.model.classes.transports.TransportAnnouncement
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel to handle creation of transport availability announcements.
 */
class TransportAnnouncementViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<AnnouncementState>(AnnouncementState.Idle)
    val state: StateFlow<AnnouncementState> = _state

    private val _announcements = MutableStateFlow<List<TransportAnnouncement>>(emptyList())
    val announcements: StateFlow<List<TransportAnnouncement>> = _announcements

    fun announce(route: Route, vehicleType: VehicleType, date: Int, cost: Double, durationMinutes: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _state.value = AnnouncementState.Error("User not logged in")
                return@launch
            }

            if (cost <= 0 || durationMinutes <= 0) {
                _state.value = AnnouncementState.Error("Invalid cost or duration")
                return@launch
            }

            val announcement = TransportAnnouncement(
                id = UUID.randomUUID().toString(),
                driverId = userId,
                vehicleType = vehicleType,
                route = route,
                date = date,
                cost = cost,
                durationMinutes = durationMinutes
            )

            _announcements.value = _announcements.value + announcement
            _state.value = AnnouncementState.Success
        }
    }

    sealed class AnnouncementState {
        object Idle : AnnouncementState()
        object Success : AnnouncementState()
        data class Error(val message: String) : AnnouncementState()
    }
}
