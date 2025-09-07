package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TripRatingEntity
import com.ioannapergamali.mysmartroute.model.classes.transports.TripWithRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel για προβολή και αποθήκευση βαθμολογιών μετακινήσεων.
 * ViewModel for displaying and storing trip ratings.
 */
class TripRatingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _trips = MutableStateFlow<List<TripWithRating>>(emptyList())
    val trips: StateFlow<List<TripWithRating>> = _trips

    /**
     * Φορτώνει ολοκληρωμένες μετακινήσεις και τις τυχόν βαθμολογίες τους.
     * Loads completed trips along with their ratings.
     */
    fun loadTrips(context: Context) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val movingFlow = db.movingDao().getAll()
            val ratingFlow = db.tripRatingDao().getAll()
            val routeFlow = db.routeDao().getAll()
            combine(movingFlow, ratingFlow, routeFlow) { movings, ratings, routes ->
                val ratingMap = ratings.associateBy { it.movingId }
                val routeMap = routes.associateBy { it.id }
                movings.filter { it.status == "completed" }.map { m ->
                    val r = ratingMap[m.id]
                    TripWithRating(
                        m.also { it.routeName = routeMap[m.routeId]?.name ?: "" },
                        r?.rating ?: 0,
                        r?.comment ?: ""
                    )
                }
            }.collect { _trips.value = it }
        }
    }

    /**
     * Ενημερώνει ή δημιουργεί βαθμολογία για μια μετακίνηση.
     * Updates or creates a rating for a trip.
     */
    fun updateRating(context: Context, moving: MovingEntity, rating: Int, comment: String) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            db.tripRatingDao().upsert(
                TripRatingEntity(moving.id, moving.userId, rating, comment)
            )
            val data = hashMapOf(
                "movingId" to moving.id,
                "userId" to moving.userId,
                "rating" to rating,
                "comment" to comment
            )
            firestore.collection("trip_ratings").document(moving.id).set(data)
        }
    }
}
