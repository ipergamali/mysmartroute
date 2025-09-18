package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TripRatingEntity
import com.ioannapergamali.mysmartroute.model.classes.transports.TripWithRating
import com.ioannapergamali.mysmartroute.repository.TripRatingRepository
import com.ioannapergamali.mysmartroute.repository.TripRatingSaveResult
import com.ioannapergamali.mysmartroute.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TripRatingViewModel : ViewModel() {

    private var repository: TripRatingRepository? = null
    private val auth = FirebaseAuth.getInstance()

    private val _trips = MutableStateFlow<List<TripWithRating>>(emptyList())
    val trips: StateFlow<List<TripWithRating>> = _trips

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadTrips(context: Context) {
        viewModelScope.launch {
            val userId = SessionManager.currentUserId(auth) ?: run {
                _trips.value = emptyList()
                return@launch
            }

            val db = MySmartRouteDatabase.getInstance(context)
            val repo = getRepository(context)
            try {
                val movings = db.movingDao().getMovingsForUser(userId).first()
                movings.forEach { moving ->
                    val local = db.tripRatingDao().get(moving.id, moving.userId)
                    if (local == null) {
                        repo.getTripRating(moving.id, moving.userId)?.let { remote ->
                            db.tripRatingDao().upsert(
                                TripRatingEntity(
                                    moving.id,
                                    remote.tripRating.userId,
                                    remote.tripRating.rating,
                                    remote.tripRating.comment ?: "",
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }

            val movingFlow = db.movingDao().getMovingsForUser(userId)
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
            val repo = getRepository(context)

            val result: TripRatingSaveResult = repo.saveTripRating(
                moving.id,
                moving.userId,
                rating,
                comment,
            )

            _message.value = when {
                result.remoteSaved -> context.getString(R.string.rating_saved_success)
                result.localSaved -> context.getString(R.string.rating_saved_offline)
                else -> context.getString(R.string.rating_save_failed)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun getRepository(context: Context): TripRatingRepository {
        val current = repository
        if (current != null) {
            return current
        }

        val db = MySmartRouteDatabase.getInstance(context)
        return TripRatingRepository(db.tripRatingDao()).also { repository = it }
    }
}

