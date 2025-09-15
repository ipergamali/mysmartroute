package com.ioannapergamali.mysmartroute.repository

import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserPoiEntity
import com.ioannapergamali.mysmartroute.data.local.insertUserPoiSafely
import com.ioannapergamali.mysmartroute.utils.SessionManager
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

    private fun userId() = SessionManager.currentUserId() ?: ""

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
     * Αντικαθιστά όλα τα αγαπημένα POI με τη νέα λίστα.
     * Replaces all favorite POIs with the provided list.
     */
    suspend fun replaceFavorites(poiIds: List<String>) {
        val uid = userId()
        if (uid.isBlank()) return
        userPoiDao.deleteAll(uid)
        poiIds.forEach { poiId ->
            val entity = UserPoiEntity(
                id = "$uid-$poiId",
                userId = uid,
                poiId = poiId
            )
            insertUserPoiSafely(userPoiDao, userDao, poiDao, entity)
        }
    }

    /**
     * Επιστρέφει τα αναγνωριστικά των αγαπημένων POIs του τρέχοντος χρήστη.
     */
    fun getFavoriteIds(): Flow<List<String>> {
        val uid = userId()
        return if (uid.isBlank()) flowOf(emptyList()) else userPoiDao.getPoiIds(uid)
    }
}
