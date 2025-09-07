package com.ioannapergamali.mysmartroute.repository

import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserPoiEntity
import com.ioannapergamali.mysmartroute.data.local.insertUserPoiSafely
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository για αποθήκευση αγαπημένων σημείων ενδιαφέροντος στην τοπική βάση (Room).
 * Τα αγαπημένα συνδέουν έναν χρήστη με ένα `PoI` μέσω της οντότητας `UserPoiEntity`.
 */
class FavoritesRepository(private val db: MySmartRouteDatabase) {
    private val userPoiDao = db.userPoiDao()
    private val userDao = db.userDao()
    private val poiDao = db.poIDao()

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    /**
     * Αποθηκεύει ένα POI ως αγαπημένο για τον τρέχοντα χρήστη.
     */
    suspend fun saveFavorite(poiId: String) {
        val uid = userId()
        if (uid.isBlank()) return
        val entity = UserPoiEntity(
            id = "$uid-$poiId",
            userId = uid,
            poiId = poiId
        )
        insertUserPoiSafely(userPoiDao, userDao, poiDao, entity)
    }

    /**
     * Αφαιρεί ένα POI από τα αγαπημένα του τρέχοντος χρήστη.
     */
    suspend fun removeFavorite(poiId: String) {
        val uid = userId()
        if (uid.isBlank()) return
        userPoiDao.delete(uid, poiId)
    }

    /**
     * Επιστρέφει τα αναγνωριστικά των αγαπημένων POIs του τρέχοντος χρήστη.
     */
    fun getFavoriteIds(): Flow<List<String>> {
        val uid = userId()
        return if (uid.isBlank()) flowOf(emptyList()) else userPoiDao.getPoiIds(uid)
    }
}
