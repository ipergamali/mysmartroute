package com.ioannapergamali.mysmartroute.viewmodel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RouteDao
import com.ioannapergamali.mysmartroute.data.local.UserDao
import com.ioannapergamali.mysmartroute.data.local.VehicleDao
import kotlinx.coroutines.Dispatchers
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toMovingEntity
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationEntity
import com.ioannapergamali.mysmartroute.utils.NotificationUtils
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.viewmodel.MainActivity
import com.ioannapergamali.mysmartroute.repository.WalkRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

data class PassengerRequest(
    val passengerId: String,
    val routeId: String,
    val fromPoiId: String,
    val toPoiId: String,
    val date: Long
)

/**
 * ViewModel για αιτήματα οχημάτων, πεζές διαδρομές και ειδοποιήσεις.
 * ViewModel handling vehicle requests, walking routes, and notifications.
 */
class VehicleRequestViewModel(
    private val walkRepository: WalkRepository = WalkRepository()
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _requests = MutableStateFlow<List<MovingEntity>>(emptyList())
    val requests: StateFlow<List<MovingEntity>> = _requests
    /**
     * Ψευδώνυμο της λίστας αιτημάτων ώστε το UI να αναφέρεται στα movings.
     * Alias for requests so UI components can refer to passenger movings directly.
     */
    val movings: StateFlow<List<MovingEntity>> = requests
    private val notifiedRequests = mutableSetOf<String>()
    private val passengerRequests = mutableSetOf<PassengerRequest>()
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications
    private val readNotificationIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "VehicleRequestVM"
        const val WALKING_ID = "WALK"
    }

    private fun getNextRequestNumber(context: Context): Int {
        val prefs = context.getSharedPreferences("vehicle_requests", Context.MODE_PRIVATE)
        val next = prefs.getInt("next_request_number", 1)
        prefs.edit().putInt("next_request_number", next + 1).apply()
        return next
    }

    /**
     * Φορτώνει αιτήματα μετακίνησης για τον τρέχοντα χρήστη ή για όλους.
     * Loads movement requests for the current user or for all users.
     */
    fun loadRequests(context: Context, allUsers: Boolean = false) {
        viewModelScope.launch {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.movingDao()
            val routeDao = dbInstance.routeDao()
            val userDao = dbInstance.userDao()
            val vehicleDao = dbInstance.vehicleDao()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            _requests.value = if (allUsers) {
                dao.getAll().first()
            } else {
                userId?.let { dao.getMovingsForUser(it).first() } ?: emptyList()
            }

            val enrichedLocal = mutableListOf<MovingEntity>()
            for (m in _requests.value) {
                enrichMoving(m, routeDao, userDao, vehicleDao)
                enrichedLocal.add(m)
            }
            _requests.value = enrichedLocal


            val snapshot = runCatching {
                if (allUsers) {
                    db.collection("movings").get().await()
                } else if (userId != null) {
                    val userRef = db.collection("users").document(userId)
                    db.collection("movings").whereEqualTo("userId", userRef).get().await()
                } else null
            }.getOrNull()

            snapshot?.let { snap ->
                val list = snap.documents.mapNotNull { it.toMovingEntity() }
                val enriched = mutableListOf<MovingEntity>()
                for (m in list) {
                    enrichMoving(m, routeDao, userDao, vehicleDao)
                    enriched.add(m)
                }
                if (enriched.isNotEmpty()) {
                    _requests.value = enriched
                    enriched.forEach { dao.insert(it) }
                }
            }

            passengerRequests.clear()
            _requests.value.forEach {
                passengerRequests.add(
                    PassengerRequest(
                        it.userId,
                        it.routeId,
                        it.startPoiId,
                        it.endPoiId,
                        it.date
                    )
                )
            }

            val notifications = if (allUsers) {
                _requests.value.filter {
                    (it.driverId.isBlank() && it.status.isBlank()) ||
                        (it.driverId == userId && it.status == "accepted")
                }
            } else {
                _requests.value.filter { it.status == "pending" }
            }
            _hasUnreadNotifications.value = notifications.any { it.id !in readNotificationIds }

            if (allUsers) {
                showPassengerRequestNotifications(context)
            } else {
                showPendingNotifications(context)
                showAcceptedNotifications(context)
                showRejectedNotifications(context)
            }
        }
    }

    private suspend fun enrichMoving(
        m: MovingEntity,
        routeDao: RouteDao,
        userDao: UserDao,
        vehicleDao: VehicleDao
    ) {
        if (m.routeName.isBlank() && m.routeId.isNotBlank()) {
            m.routeName = routeDao.findById(m.routeId)?.name ?: runCatching {
                db.collection("routes").document(m.routeId).get().await().getString("name")
            }.getOrNull().orEmpty()
        }
        if (m.driverName.isBlank() && m.driverId.isNotBlank()) {
            m.driverName = userDao.getUser(m.driverId)?.name ?: runCatching {
                db.collection("users").document(m.driverId).get().await().getString("name")
            }.getOrNull().orEmpty()
        }
        if (m.createdByName.isBlank() && m.userId.isNotBlank()) {
            m.createdByName = userDao.getUser(m.userId)?.name ?: runCatching {
                db.collection("users").document(m.userId).get().await().getString("name")
            }.getOrNull().orEmpty()
        }
        if (m.vehicleName.isBlank() && m.vehicleId.isNotBlank()) {
            m.vehicleName = vehicleDao.getVehicle(m.vehicleId)?.name ?: runCatching {
                db.collection("vehicles").document(m.vehicleId).get().await().getString("name")
            }.getOrNull().orEmpty()
        }
    }

    /**
     * Καταγράφει ένα περπάτημα ως μετακίνηση στην τοπική και απομακρυσμένη βάση.
     * Logs a walking session as a moving in local and remote storage.
     */
    fun logWalking(context: Context, dateTime: Long) {
        viewModelScope.launch {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.movingDao()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val id = UUID.randomUUID().toString()
            val entity = MovingEntity(
                id = id,
                routeId = "",
                userId = userId,
                date = dateTime,
                vehicleId = WALKING_ID,
                status = "open"
            )
            dao.insert(entity)
            try {
                val data = mapOf(
                    "id" to id,
                    "userId" to userId,
                    "date" to dateTime,
                    "vehicleId" to WALKING_ID,
                    "status" to "open"
                )
                db.collection("movings").document(id).set(data).await()

                walkRepository.startWalk(dateTime)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to log walking", e)
            }
            _requests.value = _requests.value + entity
        }
    }

    /**
     * Αποθηκεύει διαδρομή πεζοπορίας στο υποσυλλογή `walks` του χρήστη.
     * Saves a walking route to the user's `walks` subcollection.
     */
    fun saveWalkingRoute(
        context: Context,
        routeId: String,
        startTimeMillis: Long,
        walkDurationMinutes: Int
    ) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val id = UUID.randomUUID().toString()
            val routeRef = db.collection("routes").document(routeId)
            val data = mapOf(
                "routeId" to routeRef,
                "startTime" to Timestamp(Date(startTimeMillis)),
                "walkDurationMinutes" to walkDurationMinutes
            )
            try {
                val userRef = db.collection("users").document(userId)
                userRef.collection("walks").document(id).set(data).await()
                Toast.makeText(context, R.string.route_saved, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save walking route", e)
                Toast.makeText(context, R.string.route_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Σημειώνει τις ειδοποιήσεις ως αναγνωσμένες.
     * Marks notifications as read.
     */
    fun markNotificationsRead(allUsers: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val notifications = if (allUsers) {
            _requests.value.filter {
                (it.driverId.isBlank() && it.status.isBlank()) ||
                    (it.driverId == userId && it.status == "accepted")
            }
        } else {
            _requests.value.filter { it.status == "pending" }
        }
        readNotificationIds.addAll(notifications.map { it.id })
        _hasUnreadNotifications.value = false
    }

    /**
     * Διαγράφει αιτήματα μετακίνησης από τη βάση και το Firestore.
     * Deletes movement requests from the database and Firestore.
     */
    fun deleteRequests(context: Context, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            dao.deleteByIds(ids.toList())
            _requests.value = _requests.value.filterNot { it.id in ids }
            passengerRequests.clear()
            _requests.value.forEach {
                passengerRequests.add(
                    PassengerRequest(
                        it.userId,
                        it.routeId,
                        it.startPoiId,
                        it.endPoiId,
                        it.date
                    )
                )
            }
            ids.forEach { id ->
                db.collection("movings").document(id).delete()
            }
        }
    }

    /**
     * Υποβάλλει αίτημα μεταφοράς για συγκεκριμένη διαδρομή και επιβάτη.
     * Submits a transport request for a specific route and passenger.
     */
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
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.movingDao()
            val routeName = dbInstance.routeDao().findById(routeId)?.name ?: ""
            val creator = FirebaseAuth.getInstance().currentUser
            val creatorId = creator?.uid ?: ""
            val creatorName = UserViewModel().getUserName(context, creatorId)
            val userId = targetUserId ?: creatorId
            val requestKey = PassengerRequest(userId, routeId, fromPoiId, toPoiId, date)
            if (!passengerRequests.add(requestKey)) {
                Log.d(TAG, "Duplicate request ignored")
                return@launch
            }
            val id = UUID.randomUUID().toString()
            val requestNumber = getNextRequestNumber(context)
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
                createdByName = creatorName,
                requestNumber = requestNumber,
                routeName = routeName
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

    /**
     * Ενημερώνει τον επιβάτη ότι ένας οδηγός απάντησε στο αίτημά του.
     * Notifies the passenger that a driver responded to their request.
     */
    fun notifyPassenger(context: Context, requestId: String) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val driver = FirebaseAuth.getInstance().currentUser ?: return@launch
            val driverName = UserViewModel().getUserName(context, driver.uid)
            val current = _requests.value.find { it.id == requestId } ?: return@launch
            if (current.date > 0L && System.currentTimeMillis() > current.date) {
                Toast.makeText(context, R.string.request_expired, Toast.LENGTH_SHORT).show()
                return@launch
            }
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

    /**
     * Ο επιβάτης αποδέχεται ή απορρίπτει την προσφορά οδηγού.
     * Passenger accepts or rejects the driver's offer.
     */
    fun respondToOffer(context: Context, requestId: String, accept: Boolean) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val list = _requests.value.toMutableList()
            val index = list.indexOfFirst { it.id == requestId }
            if (index != -1) {
                val current = list[index]

                if (accept) {
                    val resDao = MySmartRouteDatabase.getInstance(context).seatReservationDao()

                    val declaration = try {
                        db.collection("transport_declarations")
                            .whereEqualTo(
                                "routeId",
                                db.collection("routes").document(current.routeId)
                            )
                            .whereEqualTo(
                                "driverId",
                                db.collection("users").document(current.driverId)
                            )
                            .whereEqualTo("date", current.date)
                            .limit(1)
                            .get()
                            .await()
                            .documents
                            .firstOrNull()
                            ?.toTransportDeclarationEntity()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch declaration", e)
                        null
                    }

                    val reservation = SeatReservationEntity(
                        id = UUID.randomUUID().toString(),
                        declarationId = declaration?.id ?: "",
                        routeId = current.routeId,
                        userId = current.userId,
                        date = current.date,
                        startTime = declaration?.startTime ?: 0L,
                        startPoiId = current.startPoiId,
                        endPoiId = current.endPoiId
                    )
                    resDao.insert(reservation)
                    try {
                        db.collection("seat_reservations")
                            .document(reservation.id)
                            .set(reservation.toFirestoreMap())
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create seat reservation", e)
                    }
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

    /**
     * Θέτει την κατάσταση ενός αιτήματος πεζοπορίας ως αποδεκτή ή απορριφθείσα.
     * Sets walking request status as accepted or rejected.
     */
    fun setWalkingStatus(context: Context, requestId: String, accept: Boolean) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).movingDao()
            val list = _requests.value.toMutableList()
            val index = list.indexOfFirst { it.id == requestId }
            if (index != -1) {
                val status = if (accept) "accepted" else "rejected"
                val updated = list[index].copy(status = status)
                list[index] = updated
                _requests.value = list
                dao.insert(updated)
                try {
                    db.collection("movings").document(requestId).update("status", status).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update walking status", e)
                }
            }
        }
    }

    private suspend fun showPassengerRequestNotifications(context: Context) {
        _requests.value.filter { it.driverId.isBlank() && it.status.isBlank() && it.id !in notifiedRequests }
            .forEach { req ->
                val passengerName = UserViewModel().getUserName(context, req.userId)
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("startDestination", "viewTransportRequests")
                    putExtra("requestId", req.id)
                }
                val pending = PendingIntent.getActivity(
                    context,
                    req.id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationUtils.showNotification(
                    context,
                    context.getString(R.string.notifications),
                    context.getString(
                        R.string.passenger_request_notification,
                        passengerName,
                        req.requestNumber
                    ),
                    req.id.hashCode(),
                    pending
                )
                notifiedRequests.add(req.id)
            }
    }

    private suspend fun showPendingNotifications(context: Context) {
        _requests.value.filter { it.status == "pending" && it.id !in notifiedRequests }.forEach { req ->
            val driverName = if (req.driverName.isNotBlank()) {
                req.driverName
            } else {
                UserViewModel().getUserName(context, req.driverId)
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("startDestination", "viewRequests")
                putExtra("requestId", req.id)
            }
            val pending = PendingIntent.getActivity(
                context,
                req.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationUtils.showNotification(
                context,
                context.getString(R.string.notifications),
                context.getString(
                    R.string.request_pending_notification,
                    driverName,
                    req.requestNumber
                ),
                req.id.hashCode(),
                pending
            )
            notifiedRequests.add(req.id)
        }
    }

    private suspend fun showAcceptedNotifications(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _requests.value.filter { it.status == "accepted" && it.driverId == userId && it.id !in notifiedRequests }
            .forEach { req ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("startDestination", "viewTransportRequests")
                    putExtra("requestId", req.id)
                }
                val pending = PendingIntent.getActivity(
                    context,
                    req.id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationUtils.showNotification(
                    context,
                    context.getString(R.string.notifications),
                    context.getString(R.string.request_accepted_notification, req.requestNumber),
                    req.id.hashCode(),
                    pending
                )
                notifiedRequests.add(req.id)
            }
    }

    private suspend fun showRejectedNotifications(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _requests.value.filter { it.status == "rejected" && it.driverId == userId && it.id !in notifiedRequests }
            .forEach { req ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("startDestination", "viewTransportRequests")
                    putExtra("requestId", req.id)
                }
                val pending = PendingIntent.getActivity(
                    context,
                    req.id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationUtils.showNotification(
                    context,
                    context.getString(R.string.notifications),
                    context.getString(
                        R.string.request_rejected_notification,
                        req.requestNumber
                    ),
                    req.id.hashCode(),
                    pending
                )
                notifiedRequests.add(req.id)
            }
    }
}
