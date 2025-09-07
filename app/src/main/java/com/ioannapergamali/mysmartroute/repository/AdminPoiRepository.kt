package com.ioannapergamali.mysmartroute.repository

import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository για τον διαχειριστή ώστε να ελέγχει ονόματα PoI,
 * να ενημερώνει στοιχεία και να ομαδοποιεί σημεία.
 *
 * Repository for administrators to review PoI names,
 * update their data and group related points.
 */
class AdminPoiRepository(private val db: MySmartRouteDatabase) {
    private val poiDao = db.poIDao()
    private val routePointDao = db.routePointDao()
    private val routeDao = db.routeDao()

    /**
     * Επιστροφή όλων των ονομάτων σημείων που έχουν καταχωριστεί.
     *
     * Returns all stored point names.
     */
    fun getPoiNames(): Flow<List<String>> = poiDao.getAll().map { list -> list.map { it.name } }

    /**
     * Επιστροφή όλων των σημείων.
     *
     * Returns all points.
     */
    fun getAllPois(): Flow<List<PoIEntity>> = poiDao.getAll()

    /**
     * Επιστρέφει ομάδες σημείων που μοιράζονται ίδιες συντεταγμένες
     * αλλά έχουν διαφορετικό όνομα. Χρήσιμο για εντοπισμό διπλών
     * καταχωρίσεων ώστε να συγχωνευθούν.
     *
     * Returns groups of points sharing coordinates but with different names,
     * useful to detect duplicates for merging.
     */
    fun getPoisWithSameCoordinatesDifferentName(): Flow<List<List<PoIEntity>>> =
        poiDao.getAll().map { pois ->
            pois.groupBy { it.lat to it.lng }
                .values
                .filter { group ->
                    group.size > 1 && group.map { it.name }.toSet().size > 1
                }
                .map { it }
        }

    /**
     * Ενημέρωση στοιχείων σημείου.
     *
     * Updates a point's data.
     */
    suspend fun updatePoi(poi: PoIEntity) {
        poiDao.insert(poi)
    }

    /**
     * Διαγραφή σημείου.
     *
     * Deletes a point.
     */
    suspend fun deletePoi(id: String) {
        poiDao.deleteById(id)
    }

    /**
     * Συγχώνευση δύο σημείων. Το removeId διαγράφεται και όλες οι
     * διαδρομές/σημεία που το αναφέρουν ενημερώνονται να δείχνουν στο keepId.
     *
     * Merges two points: `removeId` is deleted and all references are
     * updated to point to `keepId`.
     */
    suspend fun mergePois(keepId: String, removeId: String) {
        routePointDao.updatePoiReferences(removeId, keepId)
        routeDao.updatePoiReferences(removeId, keepId)
        poiDao.deleteById(removeId)
    }
}

