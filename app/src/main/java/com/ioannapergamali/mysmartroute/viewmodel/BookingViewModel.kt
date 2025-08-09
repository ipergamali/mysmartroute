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
            ?: run {
                Log.e(TAG, "Ο χρήστης δεν είναι συνδεδεμένος")
                return@withContext Result.failure(Exception("Ο χρήστης δεν είναι συνδεδεμένος"))
            }

        val reservationId = UUID.randomUUID().toString()
        try {
            val declarations = db.collection("transport_declarations")
                .whereEqualTo("routeId", routeId)
                .whereEqualTo("date", date)
                .get().await()
                .documents.mapNotNull { it.toTransportDeclarationEntity() }
            val declaration = declarations.firstOrNull()
                ?: run {
                    Log.e(TAG, "Δεν βρέθηκε δήλωση μεταφοράς")
                    return@withContext Result.failure(Exception("Δεν βρέθηκε δήλωση μεταφοράς"))
                }

            val routeRef = db.collection("routes").document(routeId)
            val userRef = db.collection("users").document(userId)
            val existingUserReservation = db.collection("seat_reservations")
                .whereEqualTo("routeId", routeRef)
                .whereEqualTo("date", date)
                .whereEqualTo("userId", userRef)
                .get().await()
            if (existingUserReservation.size() > 0) {
                Log.e(TAG, "Έχεις ήδη κράτηση")
                return@withContext Result.failure(Exception("Έχεις ήδη κράτηση"))
            }

            val existing = db.collection("seat_reservations")
                .whereEqualTo("routeId", routeRef)
                .whereEqualTo("date", date)
                .get().await()
            if (existing.size() >= declaration.seats) {
                Log.e(TAG, "Δεν υπάρχουν διαθέσιμες θέσεις")
                return@withContext Result.failure(Exception("Δεν υπάρχουν διαθέσιμες θέσεις"))
            }

            val dbInstance = MySmartRouteDatabase.getInstance(context)
            var points = dbInstance.routePointDao().getPointsForRoute(routeId).first()
            if (points.isEmpty()) {
                val snap = FirebaseFirestore.getInstance()
                    .collection("routes")
                    .document(routeId)
                    .get()
                    .await()
                val (_, remotePoints) = snap.toRouteWithPoints()
                    ?: run {
                        Log.e(TAG, "Αδυναμία φόρτωσης σημείων")
                        return@withContext Result.failure(Exception("Αδυναμία φόρτωσης σημείων"))
                    }
                remotePoints.forEach { dbInstance.routePointDao().insert(it) }
                points = remotePoints
            }
            val startIndex = points.indexOfFirst { it.poiId == startPoiId }
            val endIndex = points.indexOfFirst { it.poiId == endPoiId }
            if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
                Log.e(TAG, "Μη έγκυρα σημεία διαδρομής")
                return@withContext Result.failure(Exception("Μη έγκυρα σημεία διαδρομής"))
            }

            val localExisting = dbInstance.seatReservationDao()
                .findUserReservation(userId, routeId, date)
            if (localExisting != null) {
                Log.e(TAG, "Υπάρχει ήδη τοπική κράτηση")
                return@withContext Result.failure(Exception("Υπάρχει ήδη τοπική κράτηση"))
            }

            val reservation = SeatReservationEntity(
                reservationId,
                declaration.id,
                routeId,
                userId,
                date,
                startPoiId,
                endPoiId
            )
            db.collection("seat_reservations").document(reservationId)
                .set(reservation.toFirestoreMap()).await()
            dbInstance.seatReservationDao().insert(reservation)
            Log.d(TAG, "Κράτηση ολοκληρώθηκε")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Σφάλμα κατά την κράτηση", e)
            Result.failure(e)
        }
    }
}
