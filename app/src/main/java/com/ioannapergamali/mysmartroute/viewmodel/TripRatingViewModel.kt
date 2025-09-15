package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TripRatingEntity
import com.ioannapergamali.mysmartroute.model.classes.transports.TripWithRating
import com.ioannapergamali.mysmartroute.repository.TripRatingRepository
import com.ioannapergamali.mysmartroute.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripRatingViewModel : ViewModel() {

    private val repository = TripRatingRepository()
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
            try {
                val movings = db.movingDao().getMovingsForUser(userId).first()
                movings.forEach { moving ->
                    val local = db.tripRatingDao().get(moving.id, moving.userId)
                    if (local == null) {
                        repository.getTripRating(moving.id, moving.userId)?.let { remote ->
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
            val db = MySmartRouteDatabase.getInstance(context)
            val entity = TripRatingEntity(moving.id, moving.userId, rating, comment)

            val localResult = runCatching {
                withContext(Dispatchers.IO) {
                    db.tripRatingDao().upsert(entity)
                }
            }
            localResult.exceptionOrNull()?.let {
                Log.e(TAG, "Αποτυχία αποθήκευσης βαθμολογίας στη Room", it)
            }

            val remoteSuccess = try {
                repository.saveTripRating(
                    moving.id,
                    moving.userId,
                    rating,
                    comment,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Αποτυχία αποθήκευσης βαθμολογίας στο Firestore", e)
                false
            }

            _message.value = when {
                remoteSuccess -> context.getString(R.string.rating_saved_success)
                localResult.isSuccess -> context.getString(R.string.rating_saved_offline)
                else -> context.getString(R.string.rating_save_failed)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        private const val TAG = "TripRatingViewModel"
    }
}

