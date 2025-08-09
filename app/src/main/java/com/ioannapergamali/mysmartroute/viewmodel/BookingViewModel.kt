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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            ?: return@withContext Result.failure(IllegalStateException("Δεν έχει γίνει σύνδεση"))

        try {
            // Αναζήτηση δήλωσης μεταφοράς για τη συγκεκριμένη διαδρομή και ημερομηνία
            val routeRef = db.collection("routes").document(routeId)
            val declaration = db.collection("transport_declarations")
                .whereEqualTo("routeId", routeRef)
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: return@withContext Result.failure(Exception("Δεν βρέθηκε διαθέσιμη δήλωση"))

            val reservationId = UUID.randomUUID().toString()
            val entity = SeatReservationEntity(
                id = reservationId,
                declarationId = declaration.id,
                routeId = routeId,
                userId = userId,
                date = date,
                startPoiId = startPoiId,
                endPoiId = endPoiId
            )

            // Αποθήκευση τοπικά
            val dao = MySmartRouteDatabase.getInstance(context).seatReservationDao()
            dao.insert(entity)

            // Αποθήκευση στο Firestore
            db.collection("seat_reservations")
                .document(reservationId)
                .set(entity.toFirestoreMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Αποτυχία κράτησης", e)
            Result.failure(e)
        }
    }
}
