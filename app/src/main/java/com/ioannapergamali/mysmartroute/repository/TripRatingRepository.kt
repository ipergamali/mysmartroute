package com.ioannapergamali.mysmartroute.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.TripRatingDao
import com.ioannapergamali.mysmartroute.data.local.TripRatingEntity
import com.ioannapergamali.mysmartroute.model.classes.transports.TripRating
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

data class TripRatingDetails(
    val tripRating: TripRating,
    val moving: MovingInfo?,
    val user: UserInfo?
)

data class TripRatingSaveResult(
    val localSaved: Boolean,
    val remoteSaved: Boolean
)

data class MovingInfo(
    val title: String = "",
    val date: Long = 0L
)

data class UserInfo(
    val name: String = "",
    val avatarUrl: String = ""
)

/**
 * Repository για αποθήκευση βαθμολογιών ταξιδιών σε Firestore και Room.
 */
class TripRatingRepository(
    private val tripRatingDao: TripRatingDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveTripRating(
        movingId: String,
        userId: String,
        rating: Int,
        comment: String?,
    ): TripRatingSaveResult {
        val docId = "${movingId}_${userId}"
        val movingRef = db.collection("movings").document(movingId)
        val userRef = db.collection("users").document(userId)
        val data = mapOf(
            "moving" to movingRef,
            "user" to userRef,
            "rating" to rating,
            "comment" to comment
        )

        val localSaved = try {
            withContext(ioDispatcher) {
                tripRatingDao.upsert(
                    TripRatingEntity(
                        movingId = movingId,
                        userId = userId,
                        rating = rating,
                        comment = comment.orEmpty(),
                    )
                )
            }
            true
        } catch (e: Exception) {
            Log.e("TripRatingRepo", "Αποτυχία αποθήκευσης βαθμολογίας στη Room", e)
            false
        }

        val remoteSaved = try {
            db.collection("trip_ratings").document(docId).set(data).await()
            true
        } catch (e: Exception) {
            Log.e("TripRatingRepo", "Αποτυχία αποθήκευσης", e)
            false
        }

        return TripRatingSaveResult(localSaved, remoteSaved)
    }


    /**
     * Επιστρέφει τη βαθμολογία που έχει δώσει ο χρήστης για συγκεκριμένη μετακίνηση
     * μαζί με τις πληροφορίες μετακίνησης και χρήστη.
     */
    suspend fun getTripRating(movingId: String, userId: String): TripRatingDetails? {
        val docId = "${movingId}_${userId}"
        return try {
            val snap = db.collection("trip_ratings").document(docId).get().await()
            val movingRef = snap.getDocumentReference("moving")
            val userRef = snap.getDocumentReference("user")
            val rating = snap.getLong("rating")?.toInt() ?: 0
            val comment = snap.getString("comment")

            val moving = movingRef?.get()?.await()?.toObject(MovingInfo::class.java)
            val user = userRef?.get()?.await()?.toObject(UserInfo::class.java)

            TripRatingDetails(
                TripRating(movingRef?.id ?: movingId, userRef?.id ?: userId, rating, comment),
                moving,
                user
            )
        } catch (e: Exception) {
            Log.e("TripRatingRepo", "Αποτυχία ανάκτησης", e)
            null
        }
    }

}
