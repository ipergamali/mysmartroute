package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TripRatingEntity
import com.ioannapergamali.mysmartroute.model.classes.transports.TripRating
import com.ioannapergamali.mysmartroute.model.classes.transports.TripWithRating
import com.ioannapergamali.mysmartroute.repository.TripRatingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TripRatingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val repository = TripRatingRepository()

    private val _trips = MutableStateFlow<List<TripWithRating>>(emptyList())
    val trips: StateFlow<List<TripWithRating>> = _trips

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadTrips(context: Context) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            try {
                val snapshot = firestore.collection("trip_ratings").get().await()
                snapshot.documents.forEach { doc ->
                    val movingId = doc.getString("movingId") ?: return@forEach
                    val userId = doc.getString("userId") ?: ""
                    val rating = (doc.getLong("rating") ?: 0L).toInt()
                    val comment = doc.getString("comment") ?: ""
                    db.tripRatingDao().upsert(
                        TripRatingEntity(movingId, userId, rating, comment)
                    )
                }
            } catch (_: Exception) {
            }

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
                        r?.comment ?: "",
                    )
                }
            }.collect { _trips.value = it }
        }
    }

    fun saveTripRating(context: Context, moving: MovingEntity, rating: Int, comment: String) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            try {
                db.tripRatingDao().upsert(
                    TripRatingEntity(moving.id, moving.userId, rating, comment)
                )
                val success = repository.saveTripRating(
                    TripRating(moving.id, moving.userId, rating, comment)
                )
                _message.value = if (success) {
                    context.getString(R.string.rating_saved_success)
                } else {
                    context.getString(R.string.rating_save_failed)
                }
            } catch (_: Exception) {
                _message.value = context.getString(R.string.rating_save_failed)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
