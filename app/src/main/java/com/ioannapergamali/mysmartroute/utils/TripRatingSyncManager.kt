package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.utils.toTripRatingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Υπεύθυνο αντικείμενο για συγχρονισμό των αξιολογήσεων διαδρομών από το Firestore στη Room.
 * Provides a lightweight one-shot synchronisation for the trip ratings table.
 */
object TripRatingSyncManager {

    private const val TAG = "TripRatingSync"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun sync(context: Context) {
        withContext(Dispatchers.IO) {
            val db = MySmartRouteDatabase.getInstance(context)
            try {
                val snapshot = firestore.collection("trip_ratings").get().await()
                val tripRatings = snapshot.documents.mapNotNull { it.toTripRatingEntity() }
                if (tripRatings.isEmpty()) {
                    Log.d(TAG, "Δεν βρέθηκαν απομακρυσμένες αξιολογήσεις προς συγχρονισμό")
                    return@withContext
                }

                db.withTransaction {
                    val dao = db.tripRatingDao()
                    tripRatings.forEach { dao.upsert(it) }
                }
                Log.d(TAG, "Συγχρονίστηκαν ${tripRatings.size} αξιολογήσεις διαδρομών από το Firestore")
            } catch (error: Exception) {
                Log.e(TAG, "Αποτυχία συγχρονισμού αξιολογήσεων", error)
            }
        }
    }
}
