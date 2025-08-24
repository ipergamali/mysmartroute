package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.NotificationEntity
import com.ioannapergamali.mysmartroute.data.local.demoteDriverToPassenger
import com.ioannapergamali.mysmartroute.data.local.promotePassengerToDriver
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users

    private val _drivers = MutableStateFlow<List<UserEntity>>(emptyList())
    val drivers: StateFlow<List<UserEntity>> = _drivers

    fun loadUsers(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).userDao()
            var list = dao.getAllUsers().first()
            _users.value = list
            if (NetworkUtils.isInternetAvailable(context)) {
                val snapshot = runCatching {
                    db.collection("users")
                        .get()
                        .await()
                }.getOrNull()
                if (snapshot != null) {
                    val remote = snapshot.documents.mapNotNull { it.toUserEntity() }
                    remote.forEach { dao.insert(it) }
                    list = (list + remote).distinctBy { it.id }
                    _users.value = list
                }
            }
        }
    }

    fun loadDrivers(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).userDao()
            var list = dao.getAllUsers().first().filter { it.role == UserRole.DRIVER.name }
            _drivers.value = list
            if (NetworkUtils.isInternetAvailable(context)) {
                val snapshot = runCatching {
                    db.collection("users")
                        .whereEqualTo("role", UserRole.DRIVER.name)
                        .get()
                        .await()
                }.getOrNull()
                if (snapshot != null) {
                    val remote = snapshot.documents.mapNotNull { it.toUserEntity() }
                    remote.forEach { dao.insert(it) }
                    list = (list + remote).distinctBy { it.id }
                    _drivers.value = list
                }
            }
        }
    }

    suspend fun getUser(context: Context, id: String): UserEntity? {
        val dao = MySmartRouteDatabase.getInstance(context).userDao()
        val local = dao.getUser(id)
        if (local != null) return local
        return if (NetworkUtils.isInternetAvailable(context)) {
            runCatching { db.collection("users").document(id).get().await().toUserEntity() }
                .getOrNull()?.also { dao.insert(it) }
        } else null
    }

    suspend fun getUserName(context: Context, id: String): String {
        val user = getUser(context, id)
        return user?.let { "${it.name} ${it.surname}" } ?: ""
    }

    fun getNotifications(context: Context, userId: String) =
        MySmartRouteDatabase.getInstance(context).notificationDao().getForUser(userId)

    /** Διαγράφει τις ειδοποιήσεις του χρήστη αφού διαβαστούν. */
    fun markNotificationsRead(context: Context, userId: String) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).notificationDao()
            val notifications = dao.getForUser(userId).first()
            notifications.forEach { notif ->
                dao.deleteById(notif.id)
                runCatching {
                    db.collection("notifications").document(notif.id).delete().await()
                }
            }
        }
    }

    fun changeUserRole(
        context: Context,
        userId: String,
        newRole: UserRole,
        authViewModel: AuthenticationViewModel? = null
    ) {
        viewModelScope.launch {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val userDao = dbInstance.userDao()
            val user = userDao.getUser(userId) ?: return@launch
            val oldRole = runCatching { UserRole.valueOf(user.role) }.getOrNull() ?: UserRole.PASSENGER
            if (oldRole == newRole) return@launch
            user.role = newRole.name
            val newRoleId = when (newRole) {
                UserRole.PASSENGER -> "role_passenger"
                UserRole.DRIVER -> "role_driver"
                UserRole.ADMIN -> "role_admin"
            }
            user.roleId = newRoleId
            userDao.insert(user)
            runCatching {
                db.collection("users").document(userId)
                    .update("role", newRole.name, "roleId", newRoleId)
                    .await()
            }
            if (oldRole == UserRole.DRIVER && newRole == UserRole.PASSENGER) {
                handleDriverDemotion(dbInstance, userId)
            }
            if (oldRole == UserRole.PASSENGER && newRole == UserRole.DRIVER) {
                handlePassengerPromotion(dbInstance, userId)
            }
            if (FirebaseAuth.getInstance().currentUser?.uid == userId) {
                authViewModel?.loadCurrentUserRole(context, loadMenus = true)
            }
        }
    }

    private suspend fun handleDriverDemotion(dbInstance: MySmartRouteDatabase, driverId: String) {
        val seatDao = dbInstance.seatReservationDao()
        val notifDao = dbInstance.notificationDao()
        val firestore = FirebaseFirestore.getInstance()

        // Ανακτούμε όλες τις δηλώσεις μεταφοράς του οδηγού από το Firestore
        val declarations = runCatching {
            firestore.collection("transport_declarations")
                .whereEqualTo("driverId", driverId)
                .get()
                .await()
                .documents
        }.getOrNull() ?: emptyList()

        declarations.forEach { declaration ->
            // διαγράφουμε τις κρατήσεις θέσεων για κάθε δήλωση
            val reservations = seatDao.getReservationsForDeclaration(declaration.id).first()
            reservations.forEach { reservation ->
                seatDao.deleteById(reservation.id)
                runCatching {
                    firestore.collection("seat_reservations")
                        .document(reservation.id)
                        .delete()
                        .await()
                }

                // ενημερώνουμε τον επιβάτη με ειδοποίηση
                val notification = NotificationEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = reservation.userId,
                    message = "Η κράτησή σας ακυρώθηκε λόγω αλλαγής οδηγού.",
                )
                notifDao.insert(notification)
                runCatching {
                    firestore.collection("notifications")
                        .document(notification.id)
                        .set(notification.toFirestoreMap())
                        .await()
                }
            }
        }

        // Καθαρίζουμε τα δεδομένα του οδηγού από Room και Firestore
        demoteDriverToPassenger(dbInstance, driverId)
    }

    private suspend fun handlePassengerPromotion(dbInstance: MySmartRouteDatabase, userId: String) {
        promotePassengerToDriver(dbInstance, userId)
    }
}
