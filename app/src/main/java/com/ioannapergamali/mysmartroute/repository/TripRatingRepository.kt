package com.ioannapergamali.mysmartroute.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.model.classes.transports.TripRating
import kotlinx.coroutines.tasks.await

/**
 * Repository για αποθήκευση βαθμολογιών ταξιδιών στο Firestore.
 */
class TripRatingRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveTripRating(rating: TripRating): Boolean {
        val docId = "${rating.movingId}_${rating.userId}"
        return try {
            db.collection("trip_ratings").document(docId).set(rating).await()
            true
        } catch (e: Exception) {
            Log.e("TripRatingRepo", "Αποτυχία αποθήκευσης", e)
            false
        }
    }


    /**
     * Επιστρέφει τη βαθμολογία που έχει δώσει ο χρήστης για συγκεκριμένη μετακίνηση.
     */
    suspend fun getTripRating(movingId: String, userId: String): TripRating? {
        val docId = "${movingId}_${userId}"
        return try {
            db.collection("trip_ratings").document(docId).get().await()
                .toObject(TripRating::class.java)
        } catch (e: Exception) {
            Log.e("TripRatingRepo", "Αποτυχία ανάκτησης", e)
            null
        }
    }

}
