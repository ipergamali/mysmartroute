package com.ioannapergamali.mysmartroute.viewmodel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MovingDao
import com.ioannapergamali.mysmartroute.data.local.MovingDetailEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.NotificationEntity
import com.ioannapergamali.mysmartroute.data.local.RouteDao
import com.ioannapergamali.mysmartroute.data.local.UserDao
import com.ioannapergamali.mysmartroute.data.local.VehicleDao
import com.ioannapergamali.mysmartroute.data.local.TransferRequestDao
import com.ioannapergamali.mysmartroute.data.local.TransferRequestEntity
import com.ioannapergamali.mysmartroute.data.local.currentAppDateTime
import com.ioannapergamali.mysmartroute.data.local.isAwaitingDriver
import kotlinx.coroutines.Dispatchers
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toMovingEntity
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationDetailEntity
import com.ioannapergamali.mysmartroute.utils.toTransportDeclarationEntity
import com.ioannapergamali.mysmartroute.utils.NotificationUtils
import com.ioannapergamali.mysmartroute.utils.toTransferRequestEntity
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationDetailEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationDetailEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.viewmodel.MainActivity
import com.ioannapergamali.mysmartroute.repository.WalkRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import com.google.android.gms.maps.model.LatLng
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.RequestNumberProvider
import com.ioannapergamali.mysmartroute.utils.SessionManager

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

    /**
     * Φορτώνει αιτήματα μετακίνησης για τον τρέχοντα χρήστη ή για όλους.
     * Loads movement requests for the current user or for all users.
     */
    fun loadRequests(context: Context, allUsers: Boolean = false) {
        viewModelScope.launch {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.transferRequestDao()
            val routeDao = dbInstance.routeDao()
            val userDao = dbInstance.userDao()
            val vehicleDao = dbInstance.vehicleDao()
            val declarationDao = dbInstance.transportDeclarationDao()
            val declarationDetailDao = dbInstance.transportDeclarationDetailDao()
            val userId = SessionManager.currentUserId()

            val local: List<TransferRequestEntity> = if (allUsers) {
                dao.getAll().first()
            } else {
                userId?.let { dao.getRequestsForPassenger(it).first() } ?: emptyList()
            }
            val localByNumber = local.associateBy { it.requestNumber }

            val snapshot = runCatching {
                if (allUsers) {
                    db.collection("transfer_requests").get().await()
                } else if (userId != null) {
                    db.collection("transfer_requests").whereEqualTo(
                        "passengerId",
                        db.collection("users").document(userId)
                    ).get().await()
                } else null
            }.getOrNull()

            val remoteRaw = snapshot?.documents?.mapNotNull { it.toTransferRequestEntity() }.orEmpty()
            val remote: List<TransferRequestEntity> =
                if (remoteRaw.isEmpty()) {
                    emptyList()
                } else {
                    val deduped = mutableMapOf<Int, TransferRequestEntity>()
                    for (request in remoteRaw) {
                        val existing = deduped[request.requestNumber]
                        val localMatch = localByNumber[request.requestNumber]
                        val shouldReplace = when {
                            existing == null -> true
                            localMatch?.firebaseId?.isNotBlank() == true &&
                                request.firebaseId == localMatch.firebaseId -> true
                            existing.firebaseId.isBlank() && request.firebaseId.isNotBlank() -> true
                            else -> false
                        }
                        if (shouldReplace) {
                            deduped[request.requestNumber] = request
                        }
                    }
                    deduped.values.toList()
                }

            val target = when {
                remote.isNotEmpty() -> remote
                local.isNotEmpty() -> local
                else -> emptyList()
            }

            val movings = mutableListOf<MovingEntity>()
            val declarations = runCatching { declarationDao.getAll().first() }.getOrElse { emptyList() }
            val declarationsByRouteDate = declarations.groupBy { it.routeId to it.date }
            val declarationDetailsCache = mutableMapOf<String, List<TransportDeclarationDetailEntity>>()
            for (tr in target) {
                val route = routeDao.findById(tr.routeId)
                val routeDoc = if (route == null) {
                    runCatching { db.collection("routes").document(tr.routeId).get().await() }.getOrNull()
                } else null
                val startPoiId = route?.startPoiId ?: routeDoc?.getString("startPoiId").orEmpty()
                val endPoiId = route?.endPoiId ?: routeDoc?.getString("endPoiId").orEmpty()
                val resolvedDriverId = if (tr.driverId.isNotBlank()) {
                    tr.driverId
                } else {
                    val candidates = declarationsByRouteDate[tr.routeId to tr.date].orEmpty()
                    val matched = when {
                        candidates.isEmpty() -> null
                        candidates.size == 1 -> candidates.first()
                        startPoiId.isBlank() && endPoiId.isBlank() -> candidates.firstOrNull()
                        else -> {
                            candidates.firstOrNull { decl ->
                                val details = declarationDetailsCache.getOrPut(decl.id) {
                                    runCatching {
                                        declarationDetailDao.getForDeclaration(decl.id)
                                    }.getOrElse { emptyList() }
                                }
                                details.any { detail ->
                                    (startPoiId.isBlank() || detail.startPoiId == startPoiId) &&
                                        (endPoiId.isBlank() || detail.endPoiId == endPoiId)
                                }
                            } ?: candidates.firstOrNull()
                        }
                    }
                    matched?.driverId.orEmpty()
                }
                val movingId = when {
                    tr.movingId.isNotBlank() -> tr.movingId
                    tr.firebaseId.isNotBlank() -> tr.firebaseId
                    else -> "req_${tr.requestNumber}"
                }
                val moving = MovingEntity(
                    id = movingId,
                    routeId = tr.routeId,
                    userId = tr.passengerId,
                    date = tr.date,
                    vehicleId = "",
                    cost = tr.cost,
                    durationMinutes = 0,
                    startPoiId = startPoiId,
                    endPoiId = endPoiId,
                    driverId = resolvedDriverId,
                    status = tr.status.name.lowercase(),
                    requestNumber = tr.requestNumber
                ).also { it.driverName = tr.driverName }
                enrichMoving(moving, routeDao, userDao, vehicleDao)
                movings.add(moving)
            }

            _requests.value = movings

            if (remote.isNotEmpty()) {
                val existingNumbers = local.map { it.requestNumber }.toSet()
                remote.forEach { request ->
                    if (request.requestNumber in existingNumbers) {
                        dao.insert(request)
                    }
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
                    it.isAwaitingDriver() ||
                        (it.driverId == userId && it.status == "accepted")
                }
            } else {
                _requests.value.filter { it.status == "pending" && it.driverId.isNotBlank() }
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

    /**
     * Φορτώνει τις μετακινήσεις του τρέχοντος χρήστη ή όλων των χρηστών.
     * Loads movings for the current user or for all users when [allUsers] is true.
     */
    fun loadPassengerMovings(context: Context, allUsers: Boolean = false) {
        viewModelScope.launch {
            val uid = SessionManager.currentUserId()
                ?: return@launch
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.movingDao()
            val routeDao = dbInstance.routeDao()
            val userDao = dbInstance.userDao()
            val vehicleDao = dbInstance.vehicleDao()

            val localMovings = if (allUsers) {
                dao.getAll().first()
            } else {
                dao.getAll().first().filter { it.userId == uid || it.driverId == uid }
            }

            val remote = if (allUsers) {
                runCatching { db.collection("movings").get().await() }
                    .getOrNull()?.documents.orEmpty()
                    .mapNotNull { it.toMovingEntity() }
            } else {
                val passengerSnapshot = runCatching {
                    db.collection("movings").whereEqualTo(
                        "userId",
                        db.collection("users").document(uid)
                    ).get().await()
                }.getOrNull()
                val driverSnapshot = runCatching {
                    db.collection("movings").whereEqualTo(
                        "driverId",
                        db.collection("users").document(uid)
                    ).get().await()
                }.getOrNull()
                (passengerSnapshot?.documents.orEmpty() + driverSnapshot?.documents.orEmpty())
                    .mapNotNull { it.toMovingEntity() }
            }

            val target = if (remote.isNotEmpty()) remote else localMovings

            target.forEach { m ->
                enrichMoving(m, routeDao, userDao, vehicleDao)
            }

            _requests.value = target

            if (remote.isNotEmpty()) {
                remote.forEach { dao.insert(it) }
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
            val userId = SessionManager.currentUserId() ?: return@launch
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
                    "userId" to FirebaseFirestore.getInstance().collection("users").document(userId),
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
            val userId = SessionManager.currentUserId() ?: return@launch
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
        val userId = SessionManager.currentUserId()
        val notifications = if (allUsers) {
            _requests.value.filter {
                it.isAwaitingDriver() ||
                    (it.driverId == userId && it.status == "accepted")
            }
        } else {
            _requests.value.filter { it.status == "pending" && it.driverId.isNotBlank() }
        }
        readNotificationIds.addAll(notifications.map { it.id })
        _hasUnreadNotifications.value = false
    }

    /**
     * Διαγράφει αιτήματα μετακίνησης από τη βάση και το Firestore.
     * Deletes movement requests from the database and Firestore.
     */
    fun deleteRequests(context: Context, ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val movingDao = dbInstance.movingDao()
            val transferDao = dbInstance.transferRequestDao()

            val selectedRequests = _requests.value.filter { it.id in ids }
            val requestNumbers = selectedRequests
                .mapNotNull { it.requestNumber.takeIf { number -> number != 0 } }
            val transferRequests = mutableListOf<TransferRequestEntity>()

            val idsToRemoveFromRoom = mutableSetOf<String>()
            val firestoreIds = mutableSetOf<String>()

            selectedRequests.forEach { request ->
                val resolvedId = resolveMovingDocumentId(movingDao, transferDao, request)
                if (resolvedId.isNotBlank()) {
                    firestoreIds += resolvedId
                }
                idsToRemoveFromRoom += request.id
                if (resolvedId != request.id) {
                    idsToRemoveFromRoom += resolvedId
                }
            }

            requestNumbers.forEach { number ->
                val request = runCatching { transferDao.getRequestByNumber(number) }
                    .getOrNull()
                if (request != null) {
                    transferRequests += request
                }
            }

            if (idsToRemoveFromRoom.isNotEmpty()) {
                movingDao.deleteByIds(idsToRemoveFromRoom.toList())
            }
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

            if (requestNumbers.isNotEmpty()) {
                transferDao.deleteByRequestNumbers(requestNumbers)
            }

            firestoreIds.forEach { id ->
                try {
                    db.collection("movings").document(id).delete().await()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete moving $id", e)
                }
            }

            transferRequests.forEach { request ->
                try {
                    if (request.firebaseId.isNotBlank()) {
                        db.collection("transfer_requests")
                            .document(request.firebaseId)
                            .delete()
                            .await()
                    } else {
                        db.collection("transfer_requests")
                            .whereEqualTo("requestNumber", request.requestNumber)
                            .limit(1)
                            .get()
                            .await()
                            .documents
                            .firstOrNull()
                            ?.reference
                            ?.delete()
                            ?.await()
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed to delete transfer request ${request.requestNumber}",
                        e
                    )
                }
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
        maxCost: Double?,
        date: Long,
        targetUserId: String? = null
    ) {
        viewModelScope.launch {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.movingDao()
            val transferDao = dbInstance.transferRequestDao()
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
            val requestNumber = RequestNumberProvider.nextRequestNumber(transferDao, db)

            val routePoints = dbInstance.routePointDao().getPointsForRoute(routeId).first()
            val poiDao = dbInstance.poIDao()
            val pois = routePoints.mapNotNull { poiDao.findById(it.poiId) }
            val fromIdx = pois.indexOfFirst { it.id == fromPoiId }
            val toIdx = pois.indexOfFirst { it.id == toPoiId }
            val segmentPois = if (fromIdx != -1 && toIdx != -1 && fromIdx < toIdx) {
                pois.subList(fromIdx, toIdx + 1)
            } else emptyList()
            val durationMinutes = if (segmentPois.size >= 2) {
                val origin = LatLng(segmentPois.first().lat, segmentPois.first().lng)
                val destination = LatLng(segmentPois.last().lat, segmentPois.last().lng)
                val waypoints = segmentPois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
                val apiKey = MapsUtils.getApiKey(context)
                MapsUtils.fetchDuration(origin, destination, apiKey, VehicleType.CAR, waypoints)
            } else 0
            val entity = MovingEntity(
                id = id,
                routeId = routeId,
                userId = userId,
                date = date,
                vehicleId = "",
                cost = maxCost,
                durationMinutes = durationMinutes,
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
     * Στέλνει ειδοποίηση διαδρομής στον επιβάτη όταν ο οδηγός ενδιαφέρεται.
     * Sends a route notification to the passenger when a driver is interested.
     */
    fun notifyRoute(context: Context, requestId: String) {
        viewModelScope.launch {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val movingDao = dbInstance.movingDao()
            val transferDao = dbInstance.transferRequestDao()
            val driver = FirebaseAuth.getInstance().currentUser ?: return@launch
            val driverName = UserViewModel().getUserName(context, driver.uid)
            val current = _requests.value.find { it.id == requestId } ?: return@launch
            if (current.date > 0L && System.currentTimeMillis() > current.date) {
                Toast.makeText(context, R.string.request_expired, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (current.driverId == driver.uid && current.status == "pending") {
                Toast.makeText(context, R.string.request_already_pending, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val list = _requests.value.toMutableList()
            val index = list.indexOfFirst { it.id == requestId }
            if (index != -1) {
                var baseCurrent = list[index]
                val movingDocId = resolveMovingDocumentId(movingDao, transferDao, baseCurrent)
                if (movingDocId != baseCurrent.id) {
                    baseCurrent = baseCurrent.copy(id = movingDocId)
                }
                val updated = baseCurrent.copy(driverId = driver.uid, status = "pending").also {
                    it.driverName = driverName
                }
                list[index] = updated
                _requests.value = list
                movingDao.insert(updated)
                if (requestId != updated.id) {
                    runCatching { movingDao.deleteByIds(listOf(requestId)) }
                }
                try {
                    if (updated.id.isNotBlank()) {
                        db.collection("movings").document(updated.id).update(
                            mapOf(
                                "driverId" to FirebaseFirestore.getInstance().collection("users").document(driver.uid),
                                "driverName" to driverName,
                                "status" to "pending"
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send route notification", e)
                }

                val message = context.getString(
                    R.string.request_pending_notification,
                    driverName,
                    updated.requestNumber
                )
                val now = dbInstance.currentAppDateTime()
                val notificationId = "${updated.id}_${updated.userId}_pending"
                val notification = NotificationEntity(
                    id = notificationId,
                    senderId = driver.uid,
                    receiverId = updated.userId,
                    message = message,
                    sentDate = now.toLocalDate().toString(),
                    sentTime = now.toLocalTime().withSecond(0).withNano(0).toString()
                )
                try {
                    dbInstance.notificationDao().insert(notification)
                    db.collection("notifications")
                        .document(notification.id)
                        .set(notification.toFirestoreMap())
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist passenger notification", e)
                }

                if (SessionManager.currentUserId() == updated.userId) {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("startDestination", "viewRequests")
                        putExtra("requestId", updated.id)
                    }
                    val pending = PendingIntent.getActivity(
                        context,
                        updated.id.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    NotificationUtils.showNotification(
                        context,
                        context.getString(R.string.notifications),
                        message,
                        updated.id.hashCode(),
                        pending
                    )
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
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val dao = dbInstance.movingDao()
            val transferDao = dbInstance.transferRequestDao()
            val list = _requests.value.toMutableList()
            val index = list.indexOfFirst { it.id == requestId }
            if (index != -1) {
                var current = list[index]
                val resolvedId = resolveMovingDocumentId(dao, transferDao, current).ifBlank { current.id }
                if (resolvedId != current.id) {
                    current = current.copy(id = resolvedId)
                }
                var declaration: TransportDeclarationEntity? = null
                var vehicleIdFromDetails: String? = null

                if (accept) {
                    val resDao = dbInstance.seatReservationDao()
                    val resDetailDao = dbInstance.seatReservationDetailDao()
                    val detailDao = dbInstance.movingDetailDao()
                    val declarationDao = dbInstance.transportDeclarationDao()
                    val declarationDetailDao = dbInstance.transportDeclarationDetailDao()
                    val movingRef = db.collection("movings").document(current.id)

                    val relatedMovings = runCatching {
                        dao.getForRouteAndUser(current.routeId, current.userId)
                    }.getOrElse { emptyList() }
                    val uniqueMovings = (relatedMovings + current).distinctBy { it.id }
                    val needsRouteDetails = runCatching {
                        detailDao.hasDetailsForRoute(current.routeId, current.userId)
                    }.getOrDefault(false).not()
                    val movingsWithoutDetails =
                        if (needsRouteDetails) {
                            uniqueMovings.filter { moving ->
                                runCatching { detailDao.hasDetailsForMoving(moving.id) }
                                    .getOrDefault(false)
                                    .not()
                            }
                        } else {
                            emptyList()
                        }

                    val remoteMissingDetailsIds = mutableSetOf<String>()
                    val remoteMovingRefs = mutableMapOf<String, DocumentReference>()
                    val remoteVehicleFallbacks = mutableMapOf<String, String>()

                    if (current.routeId.isNotBlank() && current.userId.isNotBlank()) {
                        val routeRef = db.collection("routes").document(current.routeId)
                        val userRef = db.collection("users").document(current.userId)
                        val remoteMovingsSnap = runCatching {
                            db.collection("movings")
                                .whereEqualTo("routeId", routeRef)
                                .whereEqualTo("userId", userRef)
                                .get()
                                .await()
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to load remote movings for route ${current.routeId}", error)
                        }.getOrNull()

                        remoteMovingsSnap?.documents?.forEach { doc ->
                            val missingDetails = runCatching {
                                doc.reference.collection("details").limit(1).get().await().isEmpty()
                            }.getOrDefault(false)
                            if (missingDetails) {
                                remoteMissingDetailsIds += doc.id
                                remoteMovingRefs[doc.id] = doc.reference
                                val remoteVehicle = runCatching {
                                    doc.toMovingEntity()?.vehicleId
                                }.getOrNull()
                                if (!remoteVehicle.isNullOrBlank()) {
                                    remoteVehicleFallbacks[doc.id] = remoteVehicle
                                }
                            }
                        }
                    }

                    val requiresDeclarationDetails =
                        needsRouteDetails || remoteMissingDetailsIds.isNotEmpty()

                    declaration = runCatching {
                        if (current.driverId.isNotBlank()) {
                            declarationDao.findByRouteDriverAndDate(
                                current.routeId,
                                current.driverId,
                                current.date
                            )
                        } else {
                            null
                        }
                    }.getOrNull()

                    if (declaration == null) {
                        declaration = try {
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
                    }

                    if (declaration == null && requiresDeclarationDetails) {
                        declaration = try {
                            db.collection("transport_declarations")
                                .whereEqualTo(
                                    "routeId",
                                    db.collection("routes").document(current.routeId)
                                )
                                .limit(1)
                                .get()
                                .await()
                                .documents
                                .firstOrNull()
                                ?.toTransportDeclarationEntity()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to fetch declaration by route", e)
                            null
                        }
                    }

                    var declarationDetails: List<TransportDeclarationDetailEntity> = emptyList()
                    if (requiresDeclarationDetails && declaration != null) {
                        declarationDetails = runCatching {
                            declarationDetailDao.getForDeclaration(declaration.id)
                        }.getOrElse { emptyList() }

                        if (declarationDetails.isEmpty()) {
                            declarationDetails = try {
                                db.collection("transport_declarations")
                                    .document(declaration.id)
                                    .collection("details")
                                    .get()
                                    .await()
                                    .documents
                                    .mapNotNull {
                                        it.toTransportDeclarationDetailEntity(declaration.id)
                                    }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to load declaration details", e)
                                emptyList()
                            }
                        }
                    }

                    if (requiresDeclarationDetails && declarationDetails.isNotEmpty()) {
                        val localDetailsByMoving = mutableMapOf<String, List<MovingDetailEntity>>()
                        val remoteDetailsByMoving = mutableMapOf<String, List<MovingDetailEntity>>()

                        movingsWithoutDetails.forEach { moving ->
                            val generated = buildDetailsFromDeclaration(
                                movingId = moving.id,
                                declarationDetails = declarationDetails,
                                fallbackVehicleId = moving.vehicleId,
                                defaultVehicleId = current.vehicleId
                            )
                            if (generated.isNotEmpty()) {
                                localDetailsByMoving[moving.id] = generated
                                if (remoteMissingDetailsIds.contains(moving.id)) {
                                    remoteDetailsByMoving[moving.id] = generated
                                }
                            }
                        }

                        val remoteOnlyMovings = remoteMissingDetailsIds.filterNot { remoteDetailsByMoving.containsKey(it) }
                        remoteOnlyMovings.forEach { movingId ->
                            val fallbackVehicle = remoteVehicleFallbacks[movingId] ?: current.vehicleId
                            val generated = buildDetailsFromDeclaration(
                                movingId = movingId,
                                declarationDetails = declarationDetails,
                                fallbackVehicleId = fallbackVehicle,
                                defaultVehicleId = current.vehicleId
                            )
                            if (generated.isNotEmpty()) {
                                remoteDetailsByMoving[movingId] = generated
                            }
                        }

                        localDetailsByMoving.values.flatten().forEach { detail ->
                            try {
                                detailDao.insert(detail)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to store moving detail locally", e)
                            }
                        }

                        remoteDetailsByMoving.forEach { (movingId, details) ->
                            val targetRef = when (movingId) {
                                current.id -> movingRef
                                else -> remoteMovingRefs[movingId]
                                    ?: db.collection("movings").document(movingId)
                            }
                            details.forEach { detail ->
                                try {
                                    targetRef
                                        .collection("details")
                                        .document(detail.id)
                                        .set(detail.toFirestoreMap())
                                        .await()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to store moving detail remotely", e)
                                }
                            }
                        }

                        val chosenVehicleId = remoteDetailsByMoving[current.id]
                            ?.firstOrNull { it.vehicleId.isNotBlank() }
                            ?.vehicleId
                            ?: localDetailsByMoving[current.id]
                                ?.firstOrNull { it.vehicleId.isNotBlank() }
                                ?.vehicleId
                        if (!chosenVehicleId.isNullOrBlank()) {
                            vehicleIdFromDetails = chosenVehicleId
                        }
                    }

                    if (vehicleIdFromDetails.isNullOrBlank()) {
                        vehicleIdFromDetails = runCatching {
                            detailDao.findVehicleIdForRoute(current.routeId, current.userId)
                        }.getOrNull()
                    }

                    if (vehicleIdFromDetails.isNullOrBlank()) {
                        vehicleIdFromDetails = runCatching {
                            detailDao.getForMoving(requestId).first()
                                .firstOrNull { it.vehicleId.isNotBlank() }
                                ?.vehicleId
                        }.getOrNull()
                    }

                    val reservation = SeatReservationEntity(
                        id = UUID.randomUUID().toString(),
                        declarationId = declaration?.id ?: "",
                        routeId = current.routeId,
                        userId = current.userId,
                        date = current.date,
                        startTime = declaration?.startTime ?: 0L
                    )
                    val resDetail = SeatReservationDetailEntity(
                        id = UUID.randomUUID().toString(),
                        reservationId = reservation.id,
                        startPoiId = current.startPoiId,
                        endPoiId = current.endPoiId,
                        cost = declaration?.cost ?: 0.0,
                        durationMinutes = declaration?.durationMinutes ?: 0,
                        startTime = declaration?.startTime ?: 0L
                    )
                    resDao.insert(reservation)
                    resDetailDao.insert(resDetail)
                    try {
                        val resRef = db.collection("seat_reservations").document(reservation.id)
                        resRef.set(reservation.toFirestoreMap()).await()
                        resRef.collection("details")
                            .document(resDetail.id)
                            .set(resDetail.toFirestoreMap())
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create seat reservation", e)
                    }
                }

                val status = if (accept) "accepted" else "rejected"
                val updatedVehicleId =
                    vehicleIdFromDetails?.takeIf { it.isNotBlank() } ?: current.vehicleId
                val updated = if (accept) {
                    current.copy(
                        status = status,
                        driverId = current.driverId,
                        durationMinutes = declaration?.durationMinutes ?: current.durationMinutes,
                        vehicleId = updatedVehicleId
                    )
                } else {
                    current.copy(status = status, driverId = "")
                }
                list[index] = updated
                _requests.value = list
                dao.insert(updated)
                if (requestId != updated.id) {
                    runCatching { dao.deleteByIds(listOf(requestId)) }
                }
                try {
                    val updateMap = mutableMapOf<String, Any>(
                        "status" to status,
                        "driverId" to updated.driverId
                    )
                    if (accept && declaration != null) {
                        updateMap["durationMinutes"] = declaration.durationMinutes
                    }
                    if (updatedVehicleId.isNotBlank()) {
                        updateMap["vehicleId"] =
                            db.collection("vehicles").document(updatedVehicleId)
                    }
                    if (updated.id.isNotBlank()) {
                        db.collection("movings").document(updated.id).update(updateMap).await()
                    }
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

    private suspend fun resolveMovingDocumentId(
        movingDao: MovingDao,
        transferDao: TransferRequestDao,
        request: MovingEntity
    ): String {
        if (request.requestNumber == 0) {
            return request.id
        }

        val storedRequest = runCatching { transferDao.getRequestByNumber(request.requestNumber) }.getOrNull()
        val storedMovingId = storedRequest?.movingId?.takeIf { it.isNotBlank() }
        if (!storedMovingId.isNullOrBlank()) {
            return storedMovingId
        }

        val relatedMovings = runCatching {
            movingDao.getForRouteAndUser(request.routeId, request.userId)
        }.getOrElse { emptyList() }

        val resolvedId = relatedMovings.firstOrNull { candidate ->
            candidate.requestNumber == request.requestNumber &&
                candidate.id.isNotBlank() &&
                candidate.id != request.id
        }?.id ?: request.id

        if (resolvedId != request.id && storedRequest != null && storedRequest.movingId.isBlank()) {
            runCatching { transferDao.setMovingId(request.requestNumber, resolvedId) }
        }

        return resolvedId
    }

    private fun buildDetailsFromDeclaration(
        movingId: String,
        declarationDetails: List<TransportDeclarationDetailEntity>,
        fallbackVehicleId: String,
        defaultVehicleId: String
    ): List<MovingDetailEntity> {
        if (declarationDetails.isEmpty()) {
            return emptyList()
        }
        val normalizedFallback = fallbackVehicleId.takeIf { it.isNotBlank() } ?: ""
        val normalizedDefault = defaultVehicleId.takeIf { it.isNotBlank() } ?: ""
        return declarationDetails.map { detail ->
            val resolvedVehicleId = when {
                detail.vehicleId.isNotBlank() -> detail.vehicleId
                normalizedFallback.isNotBlank() -> normalizedFallback
                normalizedDefault.isNotBlank() -> normalizedDefault
                else -> ""
            }
            MovingDetailEntity(
                id = UUID.randomUUID().toString(),
                movingId = movingId,
                startPoiId = detail.startPoiId,
                endPoiId = detail.endPoiId,
                durationMinutes = detail.durationMinutes,
                vehicleId = resolvedVehicleId
            )
        }
    }

    private suspend fun showPassengerRequestNotifications(context: Context) {
        val receiverId = SessionManager.currentUserId() ?: return
        _requests.value.filter { it.isAwaitingDriver() && it.id !in notifiedRequests }
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
                    pending,
                    storeInRoom = true,
                    receiverId = receiverId,
                    senderId = req.userId,
                    roomNotificationId = "${req.id}_${receiverId}_awaiting_driver"
                )
                notifiedRequests.add(req.id)
            }
    }

    private suspend fun showPendingNotifications(context: Context) {
        val receiverId = SessionManager.currentUserId() ?: return
        _requests.value.filter {
            it.status == "pending" && it.driverId.isNotBlank() && it.id !in notifiedRequests
        }.forEach { req ->
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
                pending,
                storeInRoom = true,
                receiverId = receiverId,
                senderId = req.driverId,
                roomNotificationId = "${req.id}_${receiverId}_pending"
            )
            notifiedRequests.add(req.id)
        }
    }

    private suspend fun showAcceptedNotifications(context: Context) {
        val userId = SessionManager.currentUserId() ?: return
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
                    pending,
                    storeInRoom = true,
                    receiverId = userId,
                    senderId = req.userId,
                    roomNotificationId = "${req.id}_${userId}_accepted"
                )
                notifiedRequests.add(req.id)
            }
    }

    private suspend fun showRejectedNotifications(context: Context) {
        val userId = SessionManager.currentUserId() ?: return
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
                    pending,
                    storeInRoom = true,
                    receiverId = userId,
                    senderId = req.userId,
                    roomNotificationId = "${req.id}_${userId}_rejected"
                )
                notifiedRequests.add(req.id)
            }
    }
}
